Setting up Logins:

in `conf/zeppelin-site.xml`

```
/anon
<property>
  <name>zeppelin.anonymous.allowed</name>
  <value>false</value>
  <description>Anonymous user allowed by default</description>
</property>
```

in `conf/shiro.ini`

```
[users]
admin = ****, admin
demo = ****, demo
[urls]
#/** = anon
/** = authc
```
