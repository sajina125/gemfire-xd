/** Generated file. DO NOT EDIT */

#ifndef CLIENTATTRIBUTE_H_
#define CLIENTATTRIBUTE_H_

#include <set>
#include <string>

namespace com { namespace pivotal { namespace gemfirexd { namespace client {

class ClientAttribute {
private:
  // no constructors
  ClientAttribute();
  ClientAttribute(const ClientAttribute&);
  // no assignment
  ClientAttribute operator=(const ClientAttribute&);

  static std::set<std::string> s_attributes;

  static const char* addToHashSet(const char* k);

public:
  inline static const std::set<std::string>& getAllAttributes() { return s_attributes; }

  static const std::string USERNAME;
  static const std::string USERNAME_ALT;
  static const std::string PASSWORD;
  static const std::string READ_TIMEOUT;
  static const std::string KEEPALIVE_IDLE;
  static const std::string KEEPALIVE_INTVL;
  static const std::string KEEPALIVE_CNT;
  static const std::string LOAD_BALANCE;
  static const std::string SECONDARY_LOCATORS;
  static const std::string SERVER_GROUPS;
  static const std::string SINGLE_HOP_ENABLED;
  static const std::string SINGLE_HOP_MAX_CONNECTIONS;
  static const std::string DISABLE_STREAMING;
  static const std::string SKIP_LISTENERS;
  static const std::string SKIP_CONSTRAINT_CHECKS;
  static const std::string TX_SYNC_COMMITS;
  static const std::string DISABLE_THINCLIENT_CANCEL;
  static const std::string DISABLE_TX_BATCHING;
  static const std::string QUERY_HDFS;
  static const std::string LOG_FILE;
  static const std::string LOG_LEVEL;
  static const std::string LOG_FILE_STAMP;
  static const std::string CLIENT_SECURITY_MECHANISM;
  static const std::string CLIENT_TRACE_FILE;
  static const std::string CLIENT_TRACE_DIRECTORY;
  static const std::string CLIENT_TRACE_APPEND;
  static const std::string CLIENT_TRACE_LEVEL;
  static const std::string CLIENT_RETRIEVE_MESSAGE_TEXT;
  static const std::string SSL;
  static const std::string THRIFT_USE_BINARY_PROTOCOL;
  static const std::string THRIFT_SSL_PROPERTIES;

  static const int DEFAULT_LOGIN_TIMEOUT = 0;
  static const int DEFAULT_SINGLE_HOP_MAX_CONN_PER_SERVER = 5;
  static const int DEFAULT_KEEPALIVE_IDLE = 20;
  static const int DEFAULT_KEEPALIVE_INTVL = 1;
  static const int DEFAULT_KEEPALIVE_CNT = 10;
};

} } } }

#endif /* CLIENTATTRIBUTE_H_ */
