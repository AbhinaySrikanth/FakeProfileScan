"""Single feature pipeline shared by training and serving."""
FEATURES = [
    "profile_picture", "username_digit_ratio", "name_word_count", "name_digit_ratio",
    "name_equals_username", "bio_length", "has_external_url", "is_private",
    "posts", "followers", "following",
]

# Human-readable, neutral wording for each signal (used in explanations).
LABELS = {
    "profile_picture": "Profile picture",
    "username_digit_ratio": "Digits in username",
    "name_word_count": "Words in display name",
    "name_digit_ratio": "Digits in display name",
    "name_equals_username": "Display name identical to username",
    "bio_length": "Bio length",
    "has_external_url": "External link",
    "is_private": "Private account",
    "posts": "Number of posts",
    "followers": "Number of followers",
    "following": "Number of accounts followed",
}


def _digit_ratio(s: str) -> float:
    return sum(c.isdigit() for c in s) / len(s) if s else 0.0


def extract(p: dict) -> list[float]:
    """p: validated request dict -> feature vector in FEATURES order."""
    u, n = p["username"], p["full_name"]
    return [
        float(p["has_profile_picture"]),
        _digit_ratio(u),
        float(len(n.split())),
        _digit_ratio(n),
        float(n.strip().lower() == u.strip().lower()),
        float(p["bio_length"]),
        float(p["has_external_url"]),
        float(p["is_private"]),
        float(p["posts"]),
        float(p["followers"]),
        float(p["following"]),
    ]
