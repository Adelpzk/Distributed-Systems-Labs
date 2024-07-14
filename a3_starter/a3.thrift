service KeyValueService {
  string get(1: string key);
  void put(1: string key, 2: string value);

  // New method to set the primary status of the server
  void setPrimary(1: bool isPrimary);

  // New method to load key-value pairs
  void loadData(1: map<string, string> data);
}
