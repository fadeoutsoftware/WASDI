"""
RunPod Serverless handler wrapping WASDI's wasdiProcessorExecutor.py.

Maps incoming job["input"] keys to the WASDI_* environment variables expected
by wasdiProcessorExecutor.py, executes the processor in an isolated
subprocess so memory/GPU state is fully reclaimed per run, and reports
structured status back to RunPod.
"""

import json
import math
import os
import subprocess
import sys
import tempfile
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
    "WASDI_WEBSERVER_URL": ["WASDI_WEBSERVER_URL", "baseUrl", "WASDI_BASE_URL"],
}


def _extract_value(payload: Dict[str, Any], candidates: list[str]) -> Optional[Any]:
    for key in candidates:
        if key in payload and payload[key] is not None:
            return payload[key]
    return None


def _build_subprocess_env(job_input: Dict[str, Any], sBasePath: str) -> Dict[str, str]:
    asEnvironment = os.environ.copy()

    for sVariable, asKeys in ENV_KEY_MAP.items():
        oValue = _extract_value(job_input, asKeys)
        if oValue is None and sVariable == "WASDI_WEBSERVER_URL":
            oValue = asEnvironment.get(sVariable)
        asEnvironment.pop(sVariable, None)
        if sVariable == "WASDI_ONESHOT_ENCODED_PARAMS" and oValue is None:
            oParams = job_input.get("params", {})
            sJson = oParams if isinstance(oParams, str) else json.dumps(oParams)
            oValue = urllib.parse.quote(sJson)
        if not isinstance(oValue, str) or (not oValue.strip() and sVariable != "WASDI_ONESHOT_ENCODED_PARAMS"):
            raise ValueError(f"Missing or invalid {sVariable} in Runpod job configuration")
        asEnvironment[sVariable] = oValue

    asEnvironment["WASDI_ONESHOT_ON_SERVER"] = "false"
    asEnvironment["WASDI_BASE_PATH"] = sBasePath
    asEnvironment.pop("WASDI_ONLY_WS_FOLDER", None)
    asEnvironment.pop("WASDI_ONESHOT_REFRESH_PACKAGE_LIST", None)
    return asEnvironment


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
    raw_timeout = job_input.get("timeoutSeconds", job_input.get("executionTimeoutSeconds"))
    if raw_timeout is not None:
        try:
            timeout_sec = float(raw_timeout)
            if isinstance(raw_timeout, bool) or not math.isfinite(timeout_sec) or timeout_sec <= 0:
                raise ValueError()
        except (TypeError, ValueError, OverflowError):
            return {"status": "ERROR", "error": "timeoutSeconds must be a positive finite number"}

    asCommand = [sys.executable, EXECUTOR_SCRIPT, "--runpod"]

    print(f"[RunPod:{job_id}][WASDI:{proc_id}] Launching {EXECUTOR_SCRIPT}")

    try:
        # Stream stdout/stderr directly to container logs so RunPod captures live logs,
        # while enforcing timeout and checking exit code cleanly.
        with tempfile.TemporaryDirectory(prefix="wasdi-runpod-") as sBasePath:
            try:
                asEnvironment = _build_subprocess_env(job_input, sBasePath)
            except ValueError as oError:
                return {"error": str(oError), "processWorkspaceId": proc_id}
            completed = subprocess.run(
                asCommand,
                env=asEnvironment,
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

    except Exception as oError:
        sErrorType = type(oError).__name__
        print(f"[RunPod:{job_id}][WASDI:{proc_id}] UNEXPECTED ERROR: {sErrorType}")
        return {
            "error": f"Handler exception: {sErrorType}",
            "processWorkspaceId": proc_id,
        }


if __name__ == "__main__":
    runpod.serverless.start({"handler": handler})