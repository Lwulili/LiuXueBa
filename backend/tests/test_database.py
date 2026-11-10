import pytest
from alembic import command
from sqlalchemy import inspect
from sqlalchemy.exc import IntegrityError
from sqlalchemy.orm import Session

from app.models.user import User


def test_user_migration_can_be_reversed_and_reapplied(database_engine, migration_config):
    command.downgrade(migration_config, "base")
    assert "users" not in inspect(database_engine).get_table_names()
    command.upgrade(migration_config, "head")
    assert "users" in inspect(database_engine).get_table_names()
    command.check(migration_config)


@pytest.mark.parametrize("field", ["email", "username_key"])
def test_database_itself_enforces_account_uniqueness(client, database_engine, field):
    # These writes bypass the API; only the database constraint can reject the second row.
    first = User(
        username="first",
        username_key="first",
        email="first@example.com",
        nickname="first",
        password_hash="unused-test-hash",
    )
    second = User(
        username="second",
        username_key="second",
        email="second@example.com",
        nickname="second",
        password_hash="unused-test-hash",
    )
    setattr(second, field, getattr(first, field))
    with Session(database_engine) as session:
        session.add(first)
        session.commit()
        session.add(second)
        with pytest.raises(IntegrityError):
            session.commit()
        session.rollback()
