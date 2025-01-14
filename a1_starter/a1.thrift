exception IllegalArgument {
  1: string message;
}

service BcryptService {
 list<string> hashPassword (1: list<string> password, 2: i16 logRounds) throws (1: IllegalArgument e);
 list<bool> checkPassword (1: list<string> password, 2: list<string> hash) throws (1: IllegalArgument e);
 list<string> hashPasswords (1: list<string> password, 2: i16 logRounds) throws (1: IllegalArgument e);
 list<bool> checkPasswords (1: list<string> password, 2: list<string> hash) throws (1: IllegalArgument e);
 void pingFE(1: i32 port);
}
