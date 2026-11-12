import typing
from typing import Protocol, TypeVar
from typing import Protocol as P

T = TypeVar("T")


class Repo(Protocol[T]):  # generic protocol
    def get(self, key: str) -> T: ...


class UserRepo:
    def get(self, key: str) -> int: ...


class HasLen(Protocol):  # dunder-only protocol
    def __len__(self) -> int: ...


class Bag:
    def __len__(self) -> int: ...


class HasHash(Protocol):  # plain object already has __hash__
    def __hash__(self) -> int: ...


class Plain:
    def run(self) -> None: ...


class Closeable(Protocol):  # context manager dunders
    def __enter__(self) -> "Closeable": ...
    def __exit__(self, *args: object) -> None: ...


class Session:
    def __enter__(self) -> "Session": ...
    def __exit__(self, *args: object) -> None: ...


class Dynamic:  # __getattr__ answers any attribute
    def __getattr__(self, name: str) -> object: ...


class HasName(Protocol):  # attribute-only protocol
    name: str


class Person:
    name: str = ""


class Pinger(P):  # Protocol imported under another name
    def ping(self) -> None: ...


class Ponger(typing.Protocol):  # Protocol through the module
    def pong(self) -> None: ...


class Ball:
    def ping(self) -> None: ...
    def pong(self) -> None: ...


class Handler(Protocol):  # callback protocol
    def __call__(self, event: str) -> int: ...


class HandlerObject:
    def __call__(self, event: str) -> int: ...


def plain_handler(event: str) -> int: ...


async def async_handler(event: str) -> int: ...


def handler_with_default(event: str, retries: int = 3) -> int: ...


@typing.overload
def overloaded(event: str) -> int: ...
@typing.overload
def overloaded(event: bytes) -> int: ...
def overloaded(event): ...
