//spark-shell --packages eu.unicredit:hbase-rdd_2.11:0.9.1,com.databricks:spark-xml_2.11:0.11.0
import org.apache.spark.SparkFiles

val hbase_config_url = "http://xxxxxxxxxxxxxxxxxxxxxxxxxxxx:60010/conf"
spark.sparkContext.addFile(hbase_config_url)

val read_xml = (spark.read
    .format("com.databricks.spark.xml")
    .option("rootTag", "configuration")
    .option("rowTag", "property")
    .load("file://"+SparkFiles.get("conf")))
read_xml.createOrReplaceTempView("hbase_conf")

var sql = "SELECT value FROM hbase_conf where name='fs.defaultFS'"
val default_fs = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
val name_service = default_fs.split("://")(1)
sc.hadoopConfiguration.set("dfs.nameservices", sc.hadoopConfiguration.get("dfs.nameservices")+","+name_service)
sc.hadoopConfiguration.set(s"dfs.client.failover.proxy.provider.$name_service", 
    "org.apache.hadoop.hdfs.server.namenode.ha.ConfiguredFailoverProxyProvider")

sql = s"SELECT value FROM hbase_conf where name='dfs.ha.namenodes.$name_service'"
val namenodes = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
sc.hadoopConfiguration.set(s"dfs.ha.namenodes.$name_service", namenodes)

namenodes.split(",").foreach( namenode => {
    sql = s"SELECT value FROM hbase_conf where name='dfs.namenode.http-address.$name_service.$namenode'"
    val namenode_http_address = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
    sc.hadoopConfiguration.set(s"dfs.namenode.http-address.$name_service.$namenode", namenode_http_address)
    sql = s"SELECT value FROM hbase_conf where name='dfs.namenode.rpc-address.$name_service.$namenode'"
    val namenode_rpc_address = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
    sc.hadoopConfiguration.set(s"dfs.namenode.rpc-address.$name_service.$namenode", namenode_rpc_address)
    sc.hadoopConfiguration.set(s"dfs.ha.namenodes.$name_service", namenodes)
})

sql = s"SELECT value FROM hbase_conf where name='hbase.zookeeper.quorum'"
val quorum = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
sc.hadoopConfiguration.set("hbase.zookeeper.quorum", quorum)

sql = s"SELECT value FROM hbase_conf where name='hbase.zookeeper.property.clientPort'"
val clientPort = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
sc.hadoopConfiguration.set("hbase.zookeeper.property.clientPort", clientPort)

sql = s"SELECT value FROM hbase_conf where name='zookeeper.znode.parent'"
val parent = spark.sql(sql).collect.mkString("").replaceAll("[\\[\\]]","")
sc.hadoopConfiguration.set("zookeeper.znode.parent", parent)

//check property
import collection.JavaConversions._
val confAll = sc.hadoopConfiguration.iterator
confAll.map(x=>x).foreach(println)


sc.hadoopConfiguration.get("dfs.nameservices")
sc.hadoopConfiguration.get("dfs.client.failover.proxy.provider.lineaddmp1")
sc.hadoopConfiguration.get("dfs.ha.namenodes.lineaddmp1")
sc.hadoopConfiguration.get(s"dfs.namenode.http-address.lineaddmp1.namenode80")
sc.hadoopConfiguration.get(s"dfs.namenode.http-address.lineaddmp1.namenode64")
sc.hadoopConfiguration.get(s"dfs.namenode.rpc-address.lineaddmp1.namenode80")
sc.hadoopConfiguration.get(s"dfs.namenode.rpc-address.lineaddmp1.namenode64")
sc.hadoopConfiguration.get("hbase.zookeeper.quorum")
sc.hadoopConfiguration.get("hbase.zookeeper.property.clientPort")
sc.hadoopConfiguration.get("zookeeper.znode.parent")
