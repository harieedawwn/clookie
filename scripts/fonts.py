"""Build Clookie's bundled fonts from the upstream OFL sources.

Usage: python -E -P scripts/fonts.py <dir with downloaded google/fonts files>

Instances variable fonts to static weights and subsets every font to
Latin + Vietnamese so the APK stays small. Fails if any Vietnamese
character is missing. Requires: pip install -r scripts/requirements.txt
"""
import sys
from pathlib import Path

from fontTools import subset
from fontTools.ttLib import TTFont
from fontTools.varLib import instancer

OUT = Path(__file__).resolve().parent.parent / "app" / "src" / "main" / "res" / "font"

VIETNAMESE = (
    "ÀÁÂÃÈÉÊÌÍÒÓÔÕÙÚÝ"
    "àáâãèéêìíòóôõùúý"
    "ĂăĐđĨĩŨũƠơƯư"
    + "".join(chr(c) for c in range(0x1EA0, 0x1EFA))
)
UNICODES = (
    list(range(0x20, 0x7F)) + list(range(0xA0, 0x180)) + [ord(c) for c in VIETNAMESE]
    + [0x0300, 0x0301, 0x0303, 0x0309, 0x0323, 0x2013, 0x2014, 0x2018, 0x2019,
       0x201C, 0x201D, 0x2022, 0x2026, 0x2212, 0x00D7, 0x20AB, 0x2009, 0x202F]
)

# (output name, source file, axis location or None for static)
FONTS = [
    ("be_vietnam_pro_regular", "BeVietnamPro-Regular.ttf", None),
    ("be_vietnam_pro_medium", "BeVietnamPro-Medium.ttf", None),
    ("be_vietnam_pro_semibold", "BeVietnamPro-SemiBold.ttf", None),
    ("be_vietnam_pro_bold", "BeVietnamPro-Bold.ttf", None),
    # Display numerals. Compared against Bricolage Grotesque and Unbounded:
    # see design/type-display-candidates.png.
    ("fraunces_display", "Fraunces[SOFT,WONK,opsz,wght].ttf", {"wght": 800, "opsz": 144, "SOFT": 100, "WONK": 0}),
]


def build(src: Path, name: str, file: str, axes: dict | None) -> None:
    font = TTFont(src / file)
    if axes:
        font = instancer.instantiateVariableFont(font, axes, updateFontNames=False)
    missing = [c for c in VIETNAMESE if ord(c) not in font.getBestCmap()]
    if missing:
        sys.exit(f"{name}: missing Vietnamese glyphs {''.join(missing)}")
    options = subset.Options()
    options.layout_features = ["*"]
    options.name_IDs = ["*"]
    options.notdef_outline = True
    sub = subset.Subsetter(options)
    sub.populate(unicodes=UNICODES)
    sub.subset(font)
    OUT.mkdir(parents=True, exist_ok=True)
    out = OUT / f"{name}.ttf"
    font.save(out)
    print(f"{out.name}: {out.stat().st_size // 1024} KB, Vietnamese OK")


def main() -> None:
    src = Path(sys.argv[1])
    for name, file, axes in FONTS:
        build(src, name, file, axes)


if __name__ == "__main__":
    main()
