Add-Type -AssemblyName System.Drawing

$root = 'D:\onedev\app\src\main\res'
Get-ChildItem $root -Recurse -Include ic_launcher.webp, ic_launcher_round.webp -File |
    Remove-Item -Force

$sizes = @{
    'mipmap-mdpi' = 48
    'mipmap-hdpi' = 72
    'mipmap-xhdpi' = 96
    'mipmap-xxhdpi' = 144
    'mipmap-xxxhdpi' = 192
}

function New-SubscriptionIcon([string]$path, [int]$size) {
    $bitmap = New-Object System.Drawing.Bitmap $size, $size
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.Clear([System.Drawing.Color]::FromArgb(15, 31, 58))

    $overlayBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(45, 90, 154, 128))
    $graphics.FillEllipse($overlayBrush, $size * 0.52, $size * 0.58, $size * 0.58, $size * 0.58)

    $calendarX = [int]($size * 0.18)
    $calendarY = [int]($size * 0.20)
    $calendarW = [int]($size * 0.60)
    $calendarH = [int]($size * 0.56)
    $headerH = [int]($size * 0.16)

    $creamBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(255, 248, 241))
    $tealBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(90, 154, 128))
    $coralBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(233, 123, 90))
    $goldBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(200, 154, 61))
    $linePenWidth = [single]([Math]::Max(1.0, $size / 32.0))
    $linePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(50, 32, 54, 95)), $linePenWidth
    $stripeBrush = New-Object System.Drawing.SolidBrush ([System.Drawing.Color]::FromArgb(220, 255, 248, 241))

    $graphics.FillRectangle($creamBrush, $calendarX, $calendarY, $calendarW, $calendarH)
    $graphics.FillRectangle($tealBrush, $calendarX, $calendarY, $calendarW, $headerH)

    $tabW = [int]($size * 0.07)
    $tabH = [int]($size * 0.12)
    $graphics.FillRectangle($coralBrush, $calendarX + [int]($size * 0.08), [int]($size * 0.12), $tabW, $tabH)
    $graphics.FillRectangle($coralBrush, $calendarX + $calendarW - [int]($size * 0.14), [int]($size * 0.12), $tabW, $tabH)

    $gridTop = $calendarY + $headerH + [int]($size * 0.09)
    $left = $calendarX + [int]($size * 0.09)
    $right = $calendarX + $calendarW - [int]($size * 0.09)
    foreach ($offset in @(0.0, 0.14, 0.28)) {
        $y = $gridTop + [int]($size * $offset)
        $graphics.DrawLine($linePen, $left, $y, $right, $y)
    }
    foreach ($offset in @(0.14, 0.30)) {
        $x = $left + [int]($size * $offset)
        $graphics.DrawLine($linePen, $x, $gridTop - [int]($size * 0.03), $x, $gridTop + [int]($size * 0.23))
    }

    $cardX = [int]($size * 0.47)
    $cardY = [int]($size * 0.54)
    $cardW = [int]($size * 0.32)
    $cardH = [int]($size * 0.22)
    $graphics.FillRectangle($coralBrush, $cardX, $cardY, $cardW, $cardH)
    $graphics.FillRectangle($goldBrush, $cardX + [int]($size * 0.05), $cardY + [int]($size * 0.06), [int]($size * 0.08), [int]($size * 0.06))
    $graphics.FillRectangle($stripeBrush, $cardX + [int]($size * 0.04), $cardY + [int]($size * 0.14), [int]($size * 0.23), [int]($size * 0.03))

    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)

    $stripeBrush.Dispose()
    $linePen.Dispose()
    $goldBrush.Dispose()
    $coralBrush.Dispose()
    $tealBrush.Dispose()
    $creamBrush.Dispose()
    $overlayBrush.Dispose()
    $graphics.Dispose()
    $bitmap.Dispose()
}

foreach ($entry in $sizes.GetEnumerator()) {
    $dir = Join-Path $root $entry.Key
    New-SubscriptionIcon (Join-Path $dir 'ic_launcher.png') $entry.Value
    Copy-Item (Join-Path $dir 'ic_launcher.png') (Join-Path $dir 'ic_launcher_round.png') -Force
}
