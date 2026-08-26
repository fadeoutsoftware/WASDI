$ErrorActionPreference = "Stop"

$base = "https://main01.wasdi.net/ogcprocesses/rest"
$body = @{
    inputs  = @{ NAME = "teststring" }
    outputs = @{ NAME = @{ transmissionMode = "value" } }
    response = "raw"
} | ConvertTo-Json -Depth 5

$sw = [System.Diagnostics.Stopwatch]::StartNew()

Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] POST execution..."
$execResp = Invoke-WebRequest -Uri "$base/processes/hellowasdiworld/execution" -Method Post `
    -Headers @{ "Accept" = "application/json"; "Prefer" = "respond-async" } `
    -ContentType "application/json" -Body $body

Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] Status code: $($execResp.StatusCode)"
$location = $execResp.Headers["Location"]
if ($location -is [System.Array]) { $location = $location[0] }
Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] Location: $location"

$statusUrl = $location
$maxAttempts = 4
$attempt = 0
$resultsUrl = $null

while ($attempt -lt $maxAttempts) {
    $statusResp = Invoke-RestMethod -Uri $statusUrl -Method Get -Headers @{ "Accept" = "application/json" }
    Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] Poll #$attempt status: $($statusResp.status), progress: $($statusResp.progress)"

    $resultsLink = $statusResp.links | Where-Object { $_.rel -eq "http://www.opengis.net/def/rel/ogc/1.0/results" }
    if ($resultsLink) {
        $resultsUrl = $resultsLink.href
        break
    }

    if ($statusResp.status -eq "failed") {
        Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] Job failed."
        break
    }

    Start-Sleep -Seconds 5
    $attempt++
}

if ($resultsUrl) {
    Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] Fetching results..."
    $resultsResp = Invoke-RestMethod -Uri $resultsUrl -Method Get -Headers @{ "Accept" = "application/json" }
    Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] Results: $($resultsResp | ConvertTo-Json -Depth 5)"
}
else {
    Write-Host "[$($sw.Elapsed.TotalSeconds.ToString('F2'))s] No results link found within $maxAttempts polls (~$($maxAttempts * 5)s of sleep)."
}

$sw.Stop()
Write-Host "Total elapsed: $($sw.Elapsed.TotalSeconds.ToString('F2'))s"
