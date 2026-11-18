"""
RunPod Serverless handler wrapping WASDI's wasdiProcessorExecutor.py.

Maps incoming job["input"] keys to the WASDI_* environment variables expected
by wasdiProcessorExecutor.py, executes the processor in an isolated
subprocess so memory/GPU state is fully reclaimed per run, and reports
structured status back to RunPod.
"""

import json
import os
import subprocess
import sys
import traceback
import urllib.parse
from typing import Any, Dict, Optional

import runpod

EXECUTOR_SCRIPT = os.path.join(
    os.path.dirname(os.path.abspath(__file__)), "wasdiProcessorExecutor.py"
)

# Maps flexible payload keys (either explicit WASDI_* names or camelCase aliases)
# to the exact environment variables read by wasdiProcessorExecutor.py.
ENV_KEY_MAP = {
    "WASDI_PROCESS_WORKSPACE_ID": ["WASDI_PROCESS_WORKSPACE_ID", "processWorkspaceId", "procId"],
    "WASDI_USER": ["WASDI_USER", "userId", "user"],
    "WASDI_SESSION_ID": ["WASDI_SESSION_ID", "sessionId"],
    "WASDI_WORKSPACE_ID": ["WASDI_WORKSPACE_ID", "workspaceId"],
    "WASDI_ONESHOT_ENCODED_PARAMS": ["WASDI_ONESHOT_ENCODED_PARAMS", "encodedParams"],
    "WASDI_ONESHOT_ON_SERVER": ["WASDI_ONESHOT_ON_SERVER", "onServer"],
    "WASDI_ONESHOT_REFRESH_PACKAGE_LIST": [
        "WASDI_ONESHOT_REFRESH_PACKAGE_LIST",
        "refreshPackageList",
    ],
    # Optional waspy config overrides (if your waspy.init() reads them from env)
    "WASDI_BASE_URL": ["WASDI_BASE_URL", "baseUrl"],
    "WASDI_WORKSPACE_BASE_DIR": ["WASDI_WORKSPACE_BASE_DIR", "workspaceBaseDir"],
}


def _extract_value(payload: Dict[str, Any], candidates: list[str]) -> Optional[Any]:
    for key in candidates:
        if key in payload and payload[key] is not None:
            return payload[key]
    return None


def _build_subprocess_env(job_input: Dict[str, Any]) -> Dict[str, str]:
    env = os.environ.copy()

    for env_var, candidate_keys in ENV_KEY_MAP.items():
        val = _extract_value(job_input, candidate_keys)
        if val is not None:
            env[env_var] = str(val)

    # Support passing raw dict/JSON parameters via "params" if "encodedParams" isn't pre-encoded
    if "WASDI_ONESHOT_ENCODED_PARAMS" not in env and "params" in job_input:
        raw_params = job_input["params"]
        json_str = raw_params if isinstance(raw_params, str) else json.dumps(raw_params or {})
        env["WASDI_ONESHOT_ENCODED_PARAMS"] = urllib.parse.quote(json_str)

    # Default to external execution mode so waspy enables HTTP/S3 download & upload
    env.setdefault("WASDI_ONESHOT_ON_SERVER", "false")

    # Allow arbitrary extra WASDI_* keys passed inside an optional "env" sub-dict
    extra_env = job_input.get("env")
    if isinstance(extra_env, dict):
        for k, v in extra_env.items():
            if v is not None:
                env[str(k)] = str(v)

    return env


def handler(job: Dict[str, Any]) -> Dict[str, Any]:
    job_id = job.get("id", "unknown")
    job_input = job.get("input") or {}

    if not isinstance(job_input, dict):
        return {"status": "ERROR", "error": "Invalid job input: expected JSON object"}

    proc_id = str(_extract_value(job_input, ENV_KEY_MAP["WASDI_PROCESS_WORKSPACE_ID"]) or "N.A.")
    if proc_id == "N.A.":
        return {
            "status": "ERROR",
            "error": "Missing required WASDI_PROCESS_WORKSPACE_ID in job input",
        }

    # Optional soft timeout in seconds (slightly below RunPod's hard executionTimeout
    # so we can terminate the child cleanly and return diagnostics before worker kill)
    timeout_sec: Optional[float] = None
    raw_timeout = job_input.get("timeoutSeconds") or job_input.get("executionTimeoutSeconds")
    if raw_timeout is not None:
        try:
            timeout_sec = float(raw_timeout)
        except (TypeError, ValueError):
            return {"status": "ERROR", "error": f"Invalid timeoutSeconds: {raw_timeout}"}

    sub_env = _build_subprocess_env(job_input)
    cmd = [sys.executable, EXECUTOR_SCRIPT]

    print(f"[RunPod:{job_id}][WASDI:{proc_id}] Launching {EXECUTOR_SCRIPT}")

    try:
        # Stream stdout/stderr directly to container logs so RunPod captures live logs,
        # while enforcing timeout and checking exit code cleanly.
        completed = subprocess.run(
            cmd,
            env=sub_env,
            cwd=os.path.dirname(EXECUTOR_SCRIPT),
            timeout=timeout_sec,
            check=False,
        )

        rc = completed.returncode
        print(f"[RunPod:{job_id}][WASDI:{proc_id}] Finished with returncode={rc}")

        if rc != 0:
            # Returning {"error": ...} marks the RunPod job as FAILED rather than COMPLETED
            return {
                "error": f"wasdiProcessorExecutor exited with non-zero code {rc}",
                "processWorkspaceId": proc_id,
                "returncode": rc,
            }

        return {
            "status": "DONE",
            "processWorkspaceId": proc_id,
            "returncode": 0,
        }

    except subprocess.TimeoutExpired:
        msg = f"Processor exceeded timeout of {timeout_sec}s"
        print(f"[RunPod:{job_id}][WASDI:{proc_id}] TIMEOUT: {msg}")
        return {
            "error": msg,
            "processWorkspaceId": proc_id,
            "timedOut": True,
        }

    except Exception as exc:
        tb = traceback.format_exc()
        print(f"[RunPod:{job_id}][WASDI:{proc_id}] UNEXPECTED ERROR:\n{tb}")
        return {
            "error": f"Handler exception: {exc}",
            "traceback": tb,
            "processWorkspaceId": proc_id,
        }


if __name__ == "__main__":
    runpod.serverless.start({"handler": handler})