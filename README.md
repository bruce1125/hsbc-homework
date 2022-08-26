# hsbc-homework
It's a simple,self-contained authentication and authorization service.All of the api of the service are thread save.While the api are called,they return an integer code as a result as far as possible.Otherwise,we use a simple class named Result<?> to package the result,which field named "status" indicates the operation is success or not.And while success,you can achieve the real result by accessing the field which name is "retObj".

# Dependency
We use junit to develop our test cases，beside of that,it's designed based on standard JDK,that doesn't use other libraries.

# Here are the values of the return code:
| code value  | description |
| ------------- | ------------- |
| 0  | indicates the operation is ok  |
| 10000  | normally an exception occurses  |
| 10001  | the user already exists  |
| 10002  | illegal parameter  |
| 10003  | the user doesn't exist  |
| 10004  | the role already exists  |
| 10005  | the role doesn't exist  |
| 10006  | wrong password  |
| 10007  | invalid token  |
| 10008  | expired token  |
