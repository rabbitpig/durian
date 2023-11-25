#### mybatis generator使用说明

> 使用前说明
> * 库表结构必须要有以下字段约定(其中is_deleted为逻辑删除必需)

```sql
CREATE TABLE t_permission (
	id BIGINT ( 20 ) NOT NULL AUTO_INCREMENT COMMENT '主键',
	create_time time( 7 ) NOT NULL COMMENT '创建时间',
	creator_id BIGINT ( 20 ) NOT NULL COMMENT '创建人id',
	creator_name VARCHAR ( 64 ) NOT NULL COMMENT '创建人名称',
	update_time time( 7 ) NOT NULL COMMENT '修改时间',
	updator_id BIGINT ( 20 ) NOT NULL COMMENT '修改人id',
	updator_name VARCHAR ( 64 ) NOT NULL COMMENT '修改人名称',
	deleted BIGINT ( 20 ) NOT NULL COMMENT '逻辑删除',
	remark VARCHAR ( 1024 ) NOT NULL COMMENT '系统备注',
	PRIMARY KEY ( id )
) COMMENT = '权限';
```



1. 修改生成配置文件，logicalDeleteTemplate.xml（逻辑删除）或者physicalDeleteTemplate.xml（物理删除）


	```
	// 修改两处WDK_SCM_DBSYNC_APP为新应用数据库的APP NAME
	<jdbcConnection connectionURL="jdbc:mysql://tddl.daily.alibaba.net:3306/WDK_SCM_DBSYNC_APP?useUnicode=true&amp;characterEncoding=UTF-8" driverClass="org.gjt.mm.mysql.Driver" password="123456" userId="WDK_SCM_DBSYNC_APP"/>
	
	......
	
	// 新增table节点用于新表的实体类生成
	<!-- 从性能和安全考虑，建议不要开启DeleteByExample -->
   <table domainObjectName="LogicalDelete" tableName="logical_delete"  enableDeleteByExample="false">
       <generatedKey column="id" sqlStatement="JDBC"/>
   </table>
	```

2. 在dao目录下执行，逻辑删除命令（生成manager，不生成controller）

	```
	mvn -Dmybatis.generator.overwrite=true -Dmybatis.generator.configurationFile=src/main/resources/generator/config/logicalDeleteTemplate.xml mybatis-generator:generate
	```

4. 在dao目录下执行，物理删除命令（生成manager，不生成controller）

	```
	mvn -Dmybatis.generator.overwrite=true -Dmybatis.generator.configurationFile=src/main/resources/generator/config/physicalDeleteTemplate.xml mybatis-generator:generate
	```
