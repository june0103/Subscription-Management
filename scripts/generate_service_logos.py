"""
추천 서비스용 모노그램 아이콘 생성기.

실제 브랜드 로고를 앱에 넣는 대신, 서비스를 알아볼 수 있는 브랜드 색 + 이니셜 타일을 만든다.
기존 ic_service_*.png(96px)와 같은 비율(여백 5%, 모서리 23%, 글자 높이 40%)로 192px에 그린다.

사용법 (저장소 루트에서):
    python scripts/generate_service_logos.py
필요: Pillow
"""

from pathlib import Path

from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parents[1]
OUT_DIR = ROOT / "app/src/main/res/drawable-nodpi"
FONT_PATH = ROOT / "app/src/main/res/font/ibm_plex_sans_kr_bold.ttf"

SIZE = 192
INSET = round(SIZE * 5 / 96)
RADIUS = round(SIZE * 20 / 96)

# key: (배경색, 글자색, 글자). 색은 각 서비스가 쓰는 대표색에 맞췄다.
SERVICES = {
    "prime_video": ("#00A8E1", "#FFFFFF", "PV"),
    "laftel": ("#816BFF", "#FFFFFF", "L"),
    "claude_pro": ("#D97757", "#FFFFFF", "C"),
    "gemini_advanced": ("#3B6CF6", "#FFFFFF", "G"),
    "perplexity_pro": ("#20808D", "#FFFFFF", "P"),
    "copilot_pro": ("#0F6CBD", "#FFFFFF", "Co"),
    "grammarly_pro": ("#15C39A", "#FFFFFF", "G"),
    "canva_pro": ("#7D2AE8", "#FFFFFF", "C"),
    "amazon_prime": ("#232F3E", "#FF9900", "a"),
    "baemin_club": ("#2AC1BC", "#FFFFFF", "배민"),
    "yogiyo_pass": ("#FA0050", "#FFFFFF", "요"),
    "coupang_eats": ("#111111", "#00A6E3", "이츠"),
}


def text_size_for(label: str) -> int:
    # 글자 수가 많으면 줄여서 타일 안에 들어가게 한다.
    if len(label) == 1:
        return round(SIZE * 0.52)
    if len(label) == 2:
        return round(SIZE * 0.40)
    return round(SIZE * 0.30)


def render(key: str, background: str, ink: str, label: str) -> Path:
    image = Image.new("RGBA", (SIZE, SIZE), (0, 0, 0, 0))
    draw = ImageDraw.Draw(image)
    draw.rounded_rectangle(
        (INSET, INSET, SIZE - INSET - 1, SIZE - INSET - 1),
        radius=RADIUS,
        fill=background,
    )

    font = ImageFont.truetype(str(FONT_PATH), text_size_for(label))
    left, top, right, bottom = draw.textbbox((0, 0), label, font=font)
    x = (SIZE - (right - left)) / 2 - left
    y = (SIZE - (bottom - top)) / 2 - top
    draw.text((x, y), label, font=font, fill=ink)

    path = OUT_DIR / f"ic_service_{key}.png"
    image.save(path, optimize=True)
    return path


def main() -> None:
    OUT_DIR.mkdir(parents=True, exist_ok=True)
    for key, (background, ink, label) in SERVICES.items():
        print(render(key, background, ink, label).relative_to(ROOT))


if __name__ == "__main__":
    main()
