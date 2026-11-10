from sqlalchemy import create_engine
from sqlalchemy.orm import Session, sessionmaker


class Database:
    """Application-owned connection pool; each request receives its own session."""

    def __init__(self, url: str) -> None:
        self.engine = create_engine(
            url,
            pool_pre_ping=True,
            connect_args={"connect_timeout": 5},
        )
        self.sessions = sessionmaker(self.engine, class_=Session, expire_on_commit=False)

    def dispose(self) -> None:
        self.engine.dispose()
