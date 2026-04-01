$ErrorActionPreference = "Stop"
Add-Type -AssemblyName System.Drawing

$root = Split-Path -Parent $PSScriptRoot
$outputDir = Join-Path $root "marketing\play-store"
$drawableDir = Join-Path $root "app\src\main\res\drawable-nodpi"
New-Item -ItemType Directory -Force -Path $outputDir | Out-Null

function Get-Color([string]$hex) {
    [System.Drawing.ColorTranslator]::FromHtml($hex)
}

function New-Canvas([int]$width, [int]$height, [bool]$transparent = $false) {
    $format = if ($transparent) {
        [System.Drawing.Imaging.PixelFormat]::Format32bppArgb
    } else {
        [System.Drawing.Imaging.PixelFormat]::Format24bppRgb
    }
    New-Object System.Drawing.Bitmap($width, $height, $format)
}

function New-Graphics($bitmap) {
    $graphics = [System.Drawing.Graphics]::FromImage($bitmap)
    $graphics.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::HighQualityBicubic
    $graphics.CompositingQuality = [System.Drawing.Drawing2D.CompositingQuality]::HighQuality
    $graphics.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::HighQuality
    $graphics.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::AntiAliasGridFit
    $graphics
}

function New-RoundedRectPath([float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = New-Object System.Drawing.Drawing2D.GraphicsPath
    $diameter = $r * 2
    $path.AddArc($x, $y, $diameter, $diameter, 180, 90)
    $path.AddArc($x + $w - $diameter, $y, $diameter, $diameter, 270, 90)
    $path.AddArc($x + $w - $diameter, $y + $h - $diameter, $diameter, $diameter, 0, 90)
    $path.AddArc($x, $y + $h - $diameter, $diameter, $diameter, 90, 90)
    $path.CloseFigure()
    $path
}

function Fill-RoundedRect($graphics, $brush, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = New-RoundedRectPath $x $y $w $h $r
    $graphics.FillPath($brush, $path)
    $path.Dispose()
}

function Draw-RoundedRect($graphics, $pen, [float]$x, [float]$y, [float]$w, [float]$h, [float]$r) {
    $path = New-RoundedRectPath $x $y $w $h $r
    $graphics.DrawPath($pen, $path)
    $path.Dispose()
}

function Draw-Text(
    $graphics,
    [string]$text,
    [string]$fontFamily,
    [float]$size,
    $style,
    [string]$colorHex,
    [float]$x,
    [float]$y,
    [float]$w,
    [float]$h,
    [string]$align = "Near",
    [string]$lineAlign = "Near"
) {
    $font = New-Object System.Drawing.Font($fontFamily, $size, $style, [System.Drawing.GraphicsUnit]::Pixel)
    $brush = New-Object System.Drawing.SolidBrush((Get-Color $colorHex))
    $format = New-Object System.Drawing.StringFormat
    $format.Alignment = [System.Drawing.StringAlignment]::$align
    $format.LineAlignment = [System.Drawing.StringAlignment]::$lineAlign
    $graphics.DrawString($text, $font, $brush, (New-Object System.Drawing.RectangleF($x, $y, $w, $h)), $format)
    $format.Dispose()
    $brush.Dispose()
    $font.Dispose()
}

function Draw-ShadowCard($graphics, [float]$x, [float]$y, [float]$w, [float]$h, [float]$radius, [string]$fillHex) {
    $shadowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28, 10, 31, 58))
    Fill-RoundedRect $graphics $shadowBrush ($x + 8) ($y + 14) $w $h $radius
    $shadowBrush.Dispose()

    $cardBrush = New-Object System.Drawing.SolidBrush((Get-Color $fillHex))
    Fill-RoundedRect $graphics $cardBrush $x $y $w $h $radius
    $cardBrush.Dispose()

    $borderPen = New-Object System.Drawing.Pen((Get-Color "#E7DACA"), 2)
    Draw-RoundedRect $graphics $borderPen $x $y $w $h $radius
    $borderPen.Dispose()
}

function Draw-Chip($graphics, [string]$label, [float]$x, [float]$y, [float]$w, [string]$fillHex = "#ECE2D4", [string]$textHex = "#1E1B18") {
    $brush = New-Object System.Drawing.SolidBrush((Get-Color $fillHex))
    Fill-RoundedRect $graphics $brush $x $y $w 46 18
    $brush.Dispose()
    Draw-Text $graphics $label "Segoe UI" 18 ([System.Drawing.FontStyle]::Bold) $textHex $x ($y + 1) $w 42 "Center" "Center"
}

function Draw-ServiceIcon($graphics, [string]$fileName, [string]$fallbackText, [float]$x, [float]$y, [float]$size, [string]$bgHex = "#10213F") {
    $iconPath = Join-Path $drawableDir $fileName
    if ($fileName -and (Test-Path $iconPath)) {
        $icon = [System.Drawing.Image]::FromFile($iconPath)
        $graphics.DrawImage($icon, $x, $y, $size, $size)
        $icon.Dispose()
        return
    }

    $brush = New-Object System.Drawing.SolidBrush((Get-Color $bgHex))
    $graphics.FillEllipse($brush, $x, $y, $size, $size)
    $brush.Dispose()
    Draw-Text $graphics $fallbackText "Segoe UI" ($size * 0.3) ([System.Drawing.FontStyle]::Bold) "#FFFFFF" $x $y $size $size "Center" "Center"
}

function Draw-ServiceRow(
    $graphics,
    [string]$fileName,
    [string]$fallbackText,
    [string]$title,
    [string]$subtitle,
    [string]$amount,
    [string]$badge,
    [float]$x,
    [float]$y,
    [float]$w
) {
    Draw-ShadowCard $graphics $x $y $w 148 28 "#FFFFFBF6"
    Draw-ServiceIcon $graphics $fileName $fallbackText ($x + 24) ($y + 36) 64
    Draw-Text $graphics $title "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold) "#1E1B18" ($x + 110) ($y + 24) 420 40
    Draw-Text $graphics $subtitle "Segoe UI" 20 ([System.Drawing.FontStyle]::Regular) "#6F685F" ($x + 110) ($y + 70) 420 32
    if ($amount) {
        Draw-Text $graphics $amount "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold) "#1E1B18" ($x + $w - 270) ($y + 30) 190 40 "Far"
    }
    if ($badge) {
        Draw-Chip $graphics $badge ($x + $w - 154) ($y + 78) 112 "#F3E5D3" "#0F1F3A"
    }
}

function Draw-SectionTitle($graphics, [string]$title, [string]$subtitle) {
    Draw-Text $graphics $title "Segoe UI" 72 ([System.Drawing.FontStyle]::Bold) "#0F1F3A" 80 74 1080 88
    Draw-Text $graphics $subtitle "Segoe UI" 28 ([System.Drawing.FontStyle]::Regular) "#6F685F" 84 168 1080 48
}

function Save-Jpeg($bitmap, [string]$path) {
    $encoder = [System.Drawing.Imaging.ImageCodecInfo]::GetImageEncoders() | Where-Object { $_.MimeType -eq "image/jpeg" } | Select-Object -First 1
    $encoderParams = New-Object System.Drawing.Imaging.EncoderParameters(1)
    $encoderParams.Param[0] = New-Object System.Drawing.Imaging.EncoderParameter([System.Drawing.Imaging.Encoder]::Quality, 94L)
    $bitmap.Save($path, $encoder, $encoderParams)
    $encoderParams.Dispose()
}

function Draw-PlayIcon([string]$path) {
    $bitmap = New-Canvas 512 512 $true
    $graphics = New-Graphics $bitmap
    $graphics.Clear([System.Drawing.Color]::Transparent)

    $gradientRect = New-Object System.Drawing.RectangleF(0, 0, 512, 512)
    $gradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush($gradientRect, (Get-Color "#10213F"), (Get-Color "#0D7E82"), 45)
    Fill-RoundedRect $graphics $gradient 18 18 476 476 120
    $gradient.Dispose()

    $glowBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(34, 255, 255, 255))
    $graphics.FillEllipse($glowBrush, 280, 56, 168, 168)
    $glowBrush.Dispose()

    $cardBrush = New-Object System.Drawing.SolidBrush((Get-Color "#FFF7EC"))
    Fill-RoundedRect $graphics $cardBrush 92 92 328 320 64
    $cardBrush.Dispose()

    $topBrush = New-Object System.Drawing.SolidBrush((Get-Color "#E97B5A"))
    Fill-RoundedRect $graphics $topBrush 92 92 328 94 64
    $topBrush.Dispose()

    $ringPen = New-Object System.Drawing.Pen((Get-Color "#FFF7EC"), 18)
    $graphics.DrawLine($ringPen, 162, 70, 162, 134)
    $graphics.DrawLine($ringPen, 350, 70, 350, 134)
    $ringPen.Dispose()

    $innerBrush = New-Object System.Drawing.SolidBrush((Get-Color "#10213F"))
    Fill-RoundedRect $graphics $innerBrush 150 210 212 120 34
    $innerBrush.Dispose()

    $dotBrush = New-Object System.Drawing.SolidBrush((Get-Color "#D9B88B"))
    $graphics.FillEllipse($dotBrush, 178, 234, 26, 26)
    $graphics.FillEllipse($dotBrush, 216, 234, 26, 26)
    $graphics.FillEllipse($dotBrush, 254, 234, 26, 26)
    $dotBrush.Dispose()

    Draw-Text $graphics "$" "Segoe UI" 50 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 292 226 40 52 "Center" "Center"
    Draw-Text $graphics "15" "Segoe UI" 56 ([System.Drawing.FontStyle]::Bold) "#10213F" 170 344 172 52 "Center" "Center"

    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Draw-FeatureGraphic([string]$path) {
    $bitmap = New-Canvas 1024 500
    $graphics = New-Graphics $bitmap
    $appTitle = -join ([char[]](0xAD6C, 0xB3C5, 0xCCB4, 0xD06C))

    $bgRect = New-Object System.Drawing.RectangleF(0, 0, 1024, 500)
    $gradient = New-Object System.Drawing.Drawing2D.LinearGradientBrush($bgRect, (Get-Color "#F7F1E8"), (Get-Color "#F0E6D8"), 0)
    $graphics.FillRectangle($gradient, $bgRect)
    $gradient.Dispose()

    $shapeBrush = New-Object System.Drawing.SolidBrush([System.Drawing.Color]::FromArgb(28, 13, 126, 130))
    $graphics.FillEllipse($shapeBrush, 728, 34, 240, 240)
    $graphics.FillEllipse($shapeBrush, -90, 320, 330, 210)
    $shapeBrush.Dispose()

    Draw-Text $graphics $appTitle "Malgun Gothic" 62 ([System.Drawing.FontStyle]::Bold) "#0F1F3A" 72 82 420 76
    Draw-Text $graphics "Track renewals, spending, and reminders at a glance" "Segoe UI" 26 ([System.Drawing.FontStyle]::Regular) "#4E4A44" 76 168 420 74

    Draw-ShadowCard $graphics 74 286 300 126 30 "#10213F"
    Draw-Text $graphics "Monthly forecast" "Segoe UI" 20 ([System.Drawing.FontStyle]::Bold) "#D9B88B" 104 316 180 28
    Draw-Text $graphics "KRW 58,800" "Segoe UI" 34 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 104 344 220 40
    Draw-Chip $graphics "3 alerts" 244 352 88 "#E97B5A" "#FFFFFF"

    Draw-ShadowCard $graphics 496 58 452 384 38 "#FFFCF7"
    Draw-Text $graphics "April billing calendar" "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 536 92 250 36
    Draw-Text $graphics "6 payments this month" "Segoe UI" 18 ([System.Drawing.FontStyle]::Regular) "#6F685F" 740 100 180 24 "Far"

    $days = @("S", "M", "T", "W", "T", "F", "S")
    for ($i = 0; $i -lt 7; $i++) {
        Draw-Text $graphics $days[$i] "Segoe UI" 16 ([System.Drawing.FontStyle]::Bold) "#7A736B" (530 + $i * 54) 152 48 22 "Center" "Center"
    }

    $calendarRows = @(
        @("30","31","1","2","3","4","5"),
        @("6","7","8","9","10","11","12"),
        @("13","14","15","16","17","18","19"),
        @("20","21","22","23","24","25","26"),
        @("27","28","29","30","1","2","3")
    )
    for ($row = 0; $row -lt $calendarRows.Count; $row++) {
        for ($col = 0; $col -lt 7; $col++) {
            $cx = 530 + $col * 54
            $cy = 186 + $row * 44
            $selected = ($row -eq 0 -and $col -eq 3)
            if ($selected) {
                $selectedBrush = New-Object System.Drawing.SolidBrush((Get-Color "#10213F"))
                Fill-RoundedRect $graphics $selectedBrush ($cx - 4) ($cy - 4) 48 38 14
                $selectedBrush.Dispose()
            }
            $isOutside = (($row -eq 0 -and $col -lt 2) -or ($row -eq 4 -and $col -gt 3))
            $color = if ($selected) { "#FFFFFF" } elseif ($isOutside) { "#B0A59A" } else { "#1E1B18" }
            Draw-Text $graphics $calendarRows[$row][$col] "Segoe UI" 18 ([System.Drawing.FontStyle]::Bold) $color $cx $cy 40 24 "Center" "Center"
            if (($row -eq 0 -and ($col -eq 3 -or $col -eq 5)) -or ($row -eq 2 -and $col -eq 0)) {
                $dotBrush = New-Object System.Drawing.SolidBrush((Get-Color "#E97B5A"))
                $graphics.FillEllipse($dotBrush, $cx + 12, $cy + 28, 8, 8)
                $graphics.FillEllipse($dotBrush, $cx + 22, $cy + 28, 8, 8)
                $dotBrush.Dispose()
            }
        }
    }

    Draw-ServiceRow $graphics "ic_service_youtube_premium.png" "YT" "YouTube Premium" "Next bill Apr 2" "KRW 14,900" "D-3" 526 334 392
    Draw-ServiceRow $graphics "ic_service_netflix.png" "N" "Netflix" "Next bill Apr 5" "KRW 17,000" "D-6" 526 386 392

    Save-Jpeg $bitmap $path
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Draw-ScreenshotHome([string]$path) {
    $bitmap = New-Canvas 1240 2208
    $graphics = New-Graphics $bitmap
    $graphics.Clear((Get-Color "#F6F1E8"))

    Draw-SectionTitle $graphics "This month at a glance" "See upcoming bills and monthly spend fast"

    Draw-ShadowCard $graphics 70 280 1100 430 40 "#10213F"
    Draw-Text $graphics "April forecast" "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold) "#D9B88B" 118 336 240 34
    Draw-Text $graphics "KRW 58,800" "Segoe UI" 58 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 118 388 430 72
    Draw-Text $graphics "USD 24.97" "Segoe UI" 42 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 118 474 300 48
    Draw-Chip $graphics "3 alerts this week" 118 572 174 "#E97B5A" "#FFFFFF"
    Draw-Text $graphics "Closest bill Apr 2" "Segoe UI" 26 ([System.Drawing.FontStyle]::Regular) "#FFFFFF" 734 574 250 34 "Far"

    Draw-ShadowCard $graphics 70 756 530 208 32 "#FFFFFBF6"
    Draw-Text $graphics "Monthly plans" "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 110 812 240 36
    Draw-Text $graphics "8" "Segoe UI" 60 ([System.Drawing.FontStyle]::Bold) "#10213F" 110 850 110 66
    Draw-Chip $graphics "Recurring monthly" 110 910 156

    Draw-ShadowCard $graphics 640 756 530 208 32 "#FFFFFBF6"
    Draw-Text $graphics "Yearly plans" "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 680 812 220 36
    Draw-Text $graphics "3" "Segoe UI" 60 ([System.Drawing.FontStyle]::Bold) "#10213F" 680 850 110 66
    Draw-Chip $graphics "Renewal tracker" 680 910 132

    Draw-Text $graphics "Upcoming bills" "Segoe UI" 42 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 82 1020 280 50
    Draw-ServiceRow $graphics "ic_service_youtube_premium.png" "YT" "YouTube Premium" "Next bill Apr 2" "KRW 14,900" "D-3" 70 1098 1100
    Draw-ServiceRow $graphics "ic_service_netflix.png" "N" "Netflix" "Next bill Apr 5" "KRW 17,000" "D-6" 70 1268 1100
    Draw-ServiceRow $graphics "ic_service_spotify.png" "S" "Spotify" "Next bill Apr 14" "USD 10.99" "D-15" 70 1438 1100
    Draw-ServiceRow $graphics "ic_service_chatgpt_plus.png" "C" "ChatGPT Plus" "Next bill Apr 14" "USD 14.98" "D-15" 70 1608 1100

    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Draw-CalendarBoard($graphics, [float]$x, [float]$y, [float]$w) {
    $days = @("S", "M", "T", "W", "T", "F", "S")
    $cell = [math]::Floor($w / 7)
    for ($i = 0; $i -lt 7; $i++) {
        Draw-Text $graphics $days[$i] "Segoe UI" 22 ([System.Drawing.FontStyle]::Bold) "#6F685F" ($x + $i * $cell) $y $cell 34 "Center" "Center"
    }

    $rows = @(
        @("30","31","1","2","3","4","5"),
        @("6","7","8","9","10","11","12"),
        @("13","14","15","16","17","18","19"),
        @("20","21","22","23","24","25","26"),
        @("27","28","29","30","1","2","3")
    )
    $startY = $y + 54
    for ($row = 0; $row -lt $rows.Count; $row++) {
        for ($col = 0; $col -lt 7; $col++) {
            $cx = $x + $col * $cell
            $cy = $startY + $row * 104
            $isSelected = ($row -eq 0 -and $col -eq 3)
            $isCurrentMonth = !(($row -eq 0 -and $col -lt 2) -or ($row -eq 4 -and $col -gt 3))
            if ($isSelected) {
                $brush = New-Object System.Drawing.SolidBrush((Get-Color "#10213F"))
                Fill-RoundedRect $graphics $brush ($cx + 8) ($cy + 6) ($cell - 16) 74 18
                $brush.Dispose()
            }
            $color = if ($isSelected) { "#FFFFFF" } elseif ($isCurrentMonth) { "#1E1B18" } else { "#A59A8B" }
            Draw-Text $graphics $rows[$row][$col] "Segoe UI" 28 ([System.Drawing.FontStyle]::Bold) $color ($cx + 4) ($cy + 8) ($cell - 8) 30 "Center" "Center"
            if (($row -eq 0 -and ($col -eq 2 -or $col -eq 3 -or $col -eq 5)) -or ($row -eq 2 -and ($col -eq 0 -or $col -eq 1))) {
                $dotBrush = New-Object System.Drawing.SolidBrush((Get-Color "#E97B5A"))
                $graphics.FillEllipse($dotBrush, $cx + ($cell / 2) - 12, $cy + 56, 12, 12)
                $graphics.FillEllipse($dotBrush, $cx + ($cell / 2) + 4, $cy + 56, 12, 12)
                $dotBrush.Dispose()
            }
        }
    }
}

function Draw-ScreenshotCalendar([string]$path) {
    $bitmap = New-Canvas 1240 2208
    $graphics = New-Graphics $bitmap
    $graphics.Clear((Get-Color "#F6F1E8"))

    Draw-SectionTitle $graphics "Billing calendar" "See each day, selected totals, and payment details"

    Draw-ShadowCard $graphics 70 286 1100 850 40 "#FFFFFBF6"
    Draw-Text $graphics "April 2026" "Segoe UI" 42 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 112 340 220 50
    Draw-Text $graphics "6 payments this month" "Segoe UI" 24 ([System.Drawing.FontStyle]::Regular) "#6F685F" 820 348 260 32 "Far"
    Draw-CalendarBoard $graphics 110 430 1020

    Draw-ShadowCard $graphics 70 1170 1100 780 40 "#FFFFFBF6"
    Draw-Text $graphics "Thursday, Apr 2" "Segoe UI" 40 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 112 1220 330 46
    Draw-Text $graphics "2 scheduled" "Segoe UI" 24 ([System.Drawing.FontStyle]::Regular) "#6F685F" 948 1228 144 30 "Far"
    Draw-Text $graphics "KRW 14,900" "Segoe UI" 36 ([System.Drawing.FontStyle]::Bold) "#10213F" 112 1288 250 42
    Draw-Text $graphics "USD 4.99" "Segoe UI" 32 ([System.Drawing.FontStyle]::Bold) "#10213F" 390 1292 180 38
    Draw-ServiceRow $graphics "ic_service_youtube_premium.png" "YT" "YouTube Premium" "Reminder 3 days before" "KRW 14,900" "D-3" 98 1364 1044
    Draw-ServiceRow $graphics "ic_service_chatgpt_plus.png" "C" "ChatGPT Plus" "Reminder 1 day before" "USD 4.99" "D-3" 98 1534 1044

    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Draw-Field($graphics, [string]$label, [string]$value, [float]$x, [float]$y, [float]$w, [float]$h) {
    Draw-Text $graphics $label "Segoe UI" 24 ([System.Drawing.FontStyle]::Bold) "#1E1B18" $x ($y - 38) 300 28
    Draw-ShadowCard $graphics $x $y $w $h 24 "#FFFFFBF6"
    Draw-Text $graphics $value "Segoe UI" 28 ([System.Drawing.FontStyle]::Regular) "#1E1B18" ($x + 28) ($y + 20) ($w - 56) 36
}

function Draw-ScreenshotEditor([string]$path) {
    $bitmap = New-Canvas 1240 2208
    $graphics = New-Graphics $bitmap
    $graphics.Clear((Get-Color "#F6F1E8"))

    Draw-SectionTitle $graphics "Fast subscription setup" "Autocomplete services and choose currency in one screen"

    Draw-Field $graphics "Service" "You" 82 320 1076 108
    Draw-ShadowCard $graphics 82 446 1076 310 28 "#FFFFFBF6"
    Draw-ServiceRow $graphics "ic_service_youtube_premium.png" "YT" "YouTube Premium" "Suggested service" "" "" 112 476 1012
    Draw-ServiceRow $graphics "ic_service_youtube_premium.png" "YT" "YouTube" "Installed app" "" "" 112 624 1012

    Draw-Text $graphics "Currency" "Segoe UI" 24 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 82 820 300 30
    Draw-ShadowCard $graphics 82 866 528 118 24 "#FFFFFBF6"
    Draw-ShadowCard $graphics 630 866 528 118 24 "#10213F"
    Draw-Text $graphics "KRW" "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 82 905 528 42 "Center" "Center"
    Draw-Text $graphics "USD" "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 630 905 528 42 "Center" "Center"

    Draw-Field $graphics "Amount" "9.99" 82 1048 1076 108
    Draw-Text $graphics "Billing cycle" "Segoe UI" 24 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 82 1220 300 30
    Draw-ShadowCard $graphics 82 1266 528 118 24 "#10213F"
    Draw-ShadowCard $graphics 630 1266 528 118 24 "#FFFFFBF6"
    Draw-Text $graphics "Monthly" "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 82 1305 528 42 "Center" "Center"
    Draw-Text $graphics "Yearly" "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#1E1B18" 630 1305 528 42 "Center" "Center"

    Draw-Field $graphics "Billing day" "2" 82 1454 1076 108
    Draw-Field $graphics "Reminder" "3 days before" 82 1626 1076 108

    $buttonBrush = New-Object System.Drawing.SolidBrush((Get-Color "#10213F"))
    Fill-RoundedRect $graphics $buttonBrush 82 1840 1076 118 28
    $buttonBrush.Dispose()
    Draw-Text $graphics "Save" "Segoe UI" 32 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 82 1874 1076 48 "Center" "Center"

    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

function Draw-SettingRow($graphics, [string]$label, [string]$value, [float]$x, [float]$y, [float]$w) {
    Draw-ShadowCard $graphics $x $y $w 140 28 "#FFFFFBF6"
    Draw-Text $graphics $label "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#1E1B18" ($x + 28) ($y + 28) 420 40
    Draw-Text $graphics $value "Segoe UI" 24 ([System.Drawing.FontStyle]::Regular) "#6F685F" ($x + 28) ($y + 74) 560 32
}

function Draw-Switch($graphics, [float]$x, [float]$y, [bool]$enabled) {
    $fill = if ($enabled) { "#0D7E82" } else { "#D7D0C7" }
    $brush = New-Object System.Drawing.SolidBrush((Get-Color $fill))
    Fill-RoundedRect $graphics $brush $x $y 124 62 31
    $brush.Dispose()
    $knobX = if ($enabled) { $x + 64 } else { $x + 6 }
    $knobBrush = New-Object System.Drawing.SolidBrush((Get-Color "#FFFFFF"))
    $graphics.FillEllipse($knobBrush, $knobX, ($y + 6), 50, 50)
    $knobBrush.Dispose()
}

function Draw-ScreenshotSettings([string]$path) {
    $bitmap = New-Canvas 1240 2208
    $graphics = New-Graphics $bitmap
    $graphics.Clear((Get-Color "#F6F1E8"))

    Draw-SectionTitle $graphics "Reminder settings" "Send a daily summary at the time you choose"

    Draw-SettingRow $graphics "Payment reminders" "One summary alert before each billing date" 70 310 1100
    Draw-Switch $graphics 1006 352 $true

    Draw-SettingRow $graphics "Reminder time" "9:00 AM" 70 480 1100
    Draw-SettingRow $graphics "Currency display" "Keep totals separated by currency on Home and Calendar" 70 650 1100

    Draw-ShadowCard $graphics 70 868 1100 420 36 "#10213F"
    Draw-Text $graphics "Reminder preview" "Segoe UI" 30 ([System.Drawing.FontStyle]::Bold) "#D9B88B" 110 910 220 38
    Draw-Text $graphics "2 subscriptions bill tomorrow" "Segoe UI" 38 ([System.Drawing.FontStyle]::Bold) "#FFFFFF" 110 968 620 46
    Draw-Text $graphics "YouTube Premium, Netflix" "Segoe UI" 28 ([System.Drawing.FontStyle]::Regular) "#FFFFFF" 110 1036 420 34
    $ctaBrush = New-Object System.Drawing.SolidBrush((Get-Color "#F3E5D3"))
    Fill-RoundedRect $graphics $ctaBrush 110 1106 198 58 20
    $ctaBrush.Dispose()
    Draw-Text $graphics "Open app" "Segoe UI" 24 ([System.Drawing.FontStyle]::Bold) "#10213F" 110 1116 198 30 "Center" "Center"

    Draw-ServiceRow $graphics "ic_service_youtube_premium.png" "YT" "YouTube Premium" "Reminder 3 days before" "KRW 14,900" "D-1" 98 1398 1044
    Draw-ServiceRow $graphics "ic_service_netflix.png" "N" "Netflix" "Reminder 1 day before" "KRW 17,000" "D-1" 98 1568 1044

    $bitmap.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
    $graphics.Dispose()
    $bitmap.Dispose()
}

$featurePath = Join-Path $outputDir "feature-graphic-1024x500.jpg"
$iconPath = Join-Path $outputDir "play-icon-512.png"
$homePath = Join-Path $outputDir "screenshot-01-home.png"
$calendarPath = Join-Path $outputDir "screenshot-02-calendar.png"
$editorPath = Join-Path $outputDir "screenshot-03-editor.png"
$settingsPath = Join-Path $outputDir "screenshot-04-settings.png"

Draw-PlayIcon $iconPath
Draw-FeatureGraphic $featurePath
Draw-ScreenshotHome $homePath
Draw-ScreenshotCalendar $calendarPath
Draw-ScreenshotEditor $editorPath
Draw-ScreenshotSettings $settingsPath

Write-Host "Created Play Store assets:"
Write-Host " - $featurePath"
Write-Host " - $iconPath"
Write-Host " - $homePath"
Write-Host " - $calendarPath"
Write-Host " - $editorPath"
Write-Host " - $settingsPath"
