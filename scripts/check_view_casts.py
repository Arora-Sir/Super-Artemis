"""Finds findViewById results stored in or cast to a view type that the layout does not declare for that id.

A layout change (CheckBox to Switch, TextView to Button) compiles fine and only crashes with ClassCastException at runtime,
so this check runs over every layout and Java file. Exit code 1 lists each mismatch with its file and line.
"""
import pathlib
import re
import sys

ROOT = pathlib.Path(__file__).resolve().parent.parent / "app/src/main"

# Each widget class with the classes it can be assigned to (itself and its framework parents)
PARENTS = {
    "View": [],
    "TextView": ["View"],
    "EditText": ["TextView"],
    "Button": ["TextView"],
    "CompoundButton": ["Button"],
    "CheckBox": ["CompoundButton"],
    "RadioButton": ["CompoundButton"],
    "Switch": ["CompoundButton"],
    "SwitchCompat": ["CompoundButton"],
    "MaterialSwitch": ["SwitchCompat"],
    "ToggleButton": ["CompoundButton"],
    "ImageView": ["View"],
    "ImageButton": ["ImageView"],
    "ProgressBar": ["View"],
    "SeekBar": ["ProgressBar"],
    "ViewGroup": ["View"],
    "LinearLayout": ["ViewGroup"],
    "FrameLayout": ["ViewGroup"],
    "RelativeLayout": ["ViewGroup"],
    "ScrollView": ["FrameLayout"],
    "HorizontalScrollView": ["FrameLayout"],
    "NestedScrollView": ["FrameLayout"],
    "RecyclerView": ["ViewGroup"],
    "GridLayout": ["ViewGroup"],
    "CardView": ["FrameLayout"],
    "FlowRow": ["ViewGroup"],
    "Spinner": ["ViewGroup"],
}


def ancestors(cls):
    seen = [cls]
    for parent in PARENTS.get(cls, []):
        seen += ancestors(parent)
    return seen


def layout_ids():
    """id -> set of simple class names, over every layout that declares it."""
    ids = {}
    for path in (ROOT / "res").glob("layout*/*.xml"):
        text = path.read_text(encoding="utf-8")
        for match in re.finditer(r"<([\w.]+)\b([^>]*?)android:id=\"@\+id/(\w+)\"", text, re.S):
            ids.setdefault(match.group(3), set()).add(match.group(1).split(".")[-1])
    return ids


ASSIGN = re.compile(r"\b([A-Z]\w*)(?:<[^>]*>)?\s+(\w+)\s*=\s*(?:\([A-Z]\w*\)\s*)?[\w.()\[\]]*?findViewById\(R\.id\.(\w+)\)")
CAST = re.compile(r"\(\s*\(?([A-Z]\w*)\)\s*[\w.()\[\]]*?findViewById\(R\.id\.(\w+)\)")
FIELD = re.compile(r"\bfinal\s+([A-Z]\w*)\s+(\w+)\s*;")
FIELD_ASSIGN = re.compile(r"^\s*(?:this\.)?(\w+)\s*=\s*[\w.()\[\]]*?findViewById\(R\.id\.(\w+)\)", re.M)


def compatible(declared_types, java_type):
    if java_type not in PARENTS:
        return True
    return all(java_type in ancestors(t) for t in declared_types if t in PARENTS)


def main():
    ids = layout_ids()
    problems = []
    for path in (ROOT / "java").rglob("*.java"):
        text = path.read_text(encoding="utf-8")
        fields = {name: cls for cls, name in FIELD.findall(text)}
        checks = [(m.group(1), m.group(3), m.start()) for m in ASSIGN.finditer(text)]
        checks += [(m.group(1), m.group(2), m.start()) for m in CAST.finditer(text)]
        checks += [(fields[m.group(1)], m.group(2), m.start()) for m in FIELD_ASSIGN.finditer(text) if m.group(1) in fields]
        for java_type, view_id, offset in checks:
            declared = ids.get(view_id)
            if declared and not compatible(declared, java_type):
                line = text.count("\n", 0, offset) + 1
                problems.append(f"{path.relative_to(ROOT)}:{line}: R.id.{view_id} is {'/'.join(sorted(declared))} in layouts but used as {java_type}")
    for problem in sorted(set(problems)):
        print(problem)
    print(f"{len(set(problems))} view cast problem(s)")
    return 1 if problems else 0


if __name__ == "__main__":
    sys.exit(main())
