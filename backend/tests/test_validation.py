import pytest
from pydantic import ValidationError

from app.schemas.auth import LoginRequest, RegisterRequest


def registration(**overrides):
    return RegisterRequest.model_validate(
        {
            "username": "留学同学",
            "email": "student@example.com",
            "password": "study2026",
            **overrides,
        }
    )


def test_public_fields_are_trimmed_and_email_is_case_insensitive():
    user = registration(username=" 留学同学 ", email=" Student@Example.COM ")
    assert user.username == "留学同学"
    assert user.email == "student@example.com"


def test_password_is_never_trimmed():
    user = registration(password=" study2026 ")
    assert user.password.get_secret_value() == " study2026 "
    assert "study2026" not in repr(user)


@pytest.mark.parametrize("username", ["", "a", "a" * 21, "同 学", "first\tlast"])
def test_invalid_usernames_are_rejected(username):
    with pytest.raises(ValidationError):
        registration(username=username)


@pytest.mark.parametrize("username", ["同学", "a" * 20])
def test_username_length_boundaries_are_inclusive(username):
    assert registration(username=username).username == username


@pytest.mark.parametrize(
    "email",
    [
        "",
        "student",
        "a@",
        "@example.com",
        ".a@example.com",
        "a..b@example.com",
        "a@example..com",
        "a@-example.com",
        "a@exam ple.com",
        "a@example.com/path",
        "a" * 65 + "@example.com",
    ],
)
def test_invalid_email_addresses_are_rejected(email):
    with pytest.raises(ValidationError):
        registration(email=email)


@pytest.mark.parametrize("password", ["", " " * 8, "a" * 7, "a" * 65])
def test_invalid_passwords_are_rejected(password):
    with pytest.raises(ValidationError):
        registration(password=password)


@pytest.mark.parametrize("password", ["a" * 8, "a" * 64])
def test_password_length_boundaries_are_inclusive(password):
    assert registration(password=password).password.get_secret_value() == password


def test_registration_cannot_assign_admin_role():
    with pytest.raises(ValidationError):
        registration(role="admin")


def test_login_uses_the_same_email_and_password_rules():
    request = LoginRequest(email=" Student@Example.COM ", password="study2026")
    assert request.email == "student@example.com"
    with pytest.raises(ValidationError):
        LoginRequest(email="student@example.com", password="short")


@pytest.mark.parametrize("username", ["ab\x00cd", "ab\ud800cd"])
def test_usernames_reject_characters_that_cannot_be_stored(username):
    with pytest.raises(ValidationError):
        registration(username=username)


def test_password_rejects_unpaired_unicode_surrogate_before_hashing():
    with pytest.raises(ValidationError):
        registration(password="ab\ud800password")


def test_length_limits_match_android_utf16_strings():
    assert registration(username="😀", password="😀" * 4).username == "😀"
    assert registration(username="😀" * 10, password="😀" * 32).username == "😀" * 10
    with pytest.raises(ValidationError):
        registration(username="😀" * 11)
    with pytest.raises(ValidationError):
        registration(password="😀" * 33)
