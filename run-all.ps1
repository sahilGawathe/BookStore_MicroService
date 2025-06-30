$ErrorActionPreference = 'Stop'

$javaHome = 'C:\Program Files\Java\jdk-21.0.10'
$root = 'D:\BookStore-Service\bookstore-microservices'
$mvn = 'C:\Users\DELL\.m2\wrapper\dists\apache-maven-3.9.14-bin\1cb7fhup6b5n3bed6kckbrnspv\apache-maven-3.9.14\bin\mvn.cmd'

$services = @(
    'eureka-server',
    'auth-service',
    'book-service',
    'order-service',
    'api-gateway'
)

foreach ($port in 8761,8080,8081,8082,8083) {
    $connections = Get-NetTCPConnection -LocalPort $port -ErrorAction SilentlyContinue
    if ($connections) {
        foreach ($c in $connections) {
            if ($c.OwningProcess -and $c.OwningProcess -ne 0) {
                Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
            }
        }
    }
}

foreach ($svc in $services) {
    Start-Process -FilePath $mvn -ArgumentList 'spring-boot:run' -WorkingDirectory (Join-Path $root $svc) -NoNewWindow
    Start-Sleep -Seconds 2
}

Write-Host 'All services started. Check http://localhost:8761'
