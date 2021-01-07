package site.autzone.sqlbee.sql.query;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import site.autzone.sqlbee.column.Column;
import site.autzone.sqlbee.IValue;
import site.autzone.sqlbee.condition.Condition;
import site.autzone.sqlbee.condition.Operator;
import site.autzone.sqlbee.sql.Table;
import site.autzone.sqlbee.value.Value;
import site.autzone.sqlbee.sql.Sql;
import site.autzone.sqlbee.builder.SqlBuilder;

/**
 * sql查询构建器测试用例
 *
 * @author wisesean
 */
public class SqlQueryBuilderTest {

    @Test
    public void demo() {
        SqlBuilder.createQuery().table("TABLE1", "T1").sql();
        SqlBuilder.createQuery().table("TABLE1", "T1").maxResults(300).sql();
        SqlBuilder.createQuery()
                .table("TABLE1", "T1")
                .firstResults(100)
                .maxResults(300)
                .condition("=")
                .left("T1.CODE")
                .right(new Value("000000"))
                .end()
                .condition("=")
                .left("T1.is_new")
                .right(new Value(true))
                .end()
                .sql();
    }

    @Test
    public void groupHavingOrderTest() {
        SqlBuilder sb = SqlBuilder
                .createQuery().table("TABLE1", "T1")
                .groupBy("T1.CODE")
                .groupBy("T1.CLASS")
                .having(new Condition(Operator.GT.operator(), "COUNt(*)", new Value(2, Integer.class)))
                .order("T1.ID").desc().end();
        Sql sql = sb.build();
		Assert.assertEquals("SELECT * FROM TABLE1 AS T1 GROUP BY T1.CODE,T1.CLASS HAVING (COUNt(*) > ?) ORDER BY T1.ID DESC",
                sql.output());
        Assert.assertEquals(2, sql.getParameters().get(0));
    }

    @Test
    public void existsTest() {
        SqlBuilder sb = SqlBuilder.createQuery()
                .table("TABLE1", "T1").end()
                .condition(">").left("T1.CODE").right(new Value(111)).end()
                .exists().sub(
                        SqlBuilder.createSubQuery()
                                .table("TABLE2", "T2").end()
                                .condition("=").left("T2.NAME").right(new Value("测试")).end()
                                .condition("=").left("T1.ID").right("T2.ID").end()
                                .build())
                .end()
                .condition("=").left("T1.CLASS").right(new Value("ABC")).end();
        Sql sql = sb.build();
        Assert.assertEquals("SELECT * FROM TABLE1 AS T1 WHERE (T1.CODE > ?) AND (T1.CLASS = ?) AND EXISTS (SELECT * FROM TABLE2 AS T2 WHERE (T2.NAME = ?) AND (T1.ID = T2.ID))",
                sql.output());
        Assert.assertEquals(111, sql.getParameters().get(0));
        Assert.assertEquals("ABC", sql.getParameters().get(1));
        Assert.assertEquals("测试", sql.getParameters().get(2));
    }

    @Test
    public void notExistsTest() {
        SqlBuilder sb = SqlBuilder.createQuery().table("TABLE1", "T1").end()
                .notExists().sub(
                        SqlBuilder.createQuery()
                                .table("TABLE2", "T2").end()
                                .condition("=").left("T2.NAME").right(new Value("测试")).end()
                                .condition("=").left("T1.ID").right("T2.ID").end()
                                .build())
                .end();
        Assert.assertEquals("SELECT * FROM TABLE1 AS T1 WHERE " +
                        "NOT EXISTS (SELECT * FROM TABLE2 AS T2 WHERE (T2.NAME = ?) AND (T1.ID = T2.ID))",
                sb.build().output());
    }

    @Test
    public void testLeftJoinApp() throws Exception {
        SqlBuilder sb = SqlBuilder.createQuery().table("TABLE1", "T1").column("T1.*")
                .leftJoin(new Table("TABLE2", "T2")).joinCondition("T1.ID", "T2.ID").leftJoin(new Table("TABLE4", "T4"))
                .joinCondition("T1.ID", "T4.ID").end();
        Sql sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.* FROM TABLE1 AS T1 LEFT JOIN TABLE2 AS T2 ON (T1.ID = T2.ID) LEFT JOIN TABLE4 AS T4 ON (T1.ID = T4.ID)",
                sqlQuery.output());

        sb.table().leftJoin(new Table("TABLE4", "T4")).joinCondition("T3.ID", new Value("1"))
                .end();
        sb.condition("LIKE").left("T1.NAME").right(new Value("test")).end();
        sqlQuery = sb.build();
        sqlQuery.output();
    }

    @Test
    public void testRightJoinApp() throws Exception {
        SqlBuilder sb = SqlBuilder.createQuery().table("TABLE1", "T1").rightJoin(new Table("TABLE2", "T2"))
                .joinCondition("T1.ID", "T2.ID").end().select().column("T1.*").end();
        Sql sqlQuery = sb.build();
        Assert.assertEquals("SELECT T1.* FROM TABLE1 AS T1 RIGHT JOIN TABLE2 AS T2 ON (T1.ID = T2.ID)", sqlQuery.output());

        sb.table().rightJoin(new Table(1, "TABLE4", "T4")).joinCondition("T3.ID", new Value("1"))
                .end();
        sb.condition("LIKE").left("T1.NAME").right(new Value("test")).end();
        sqlQuery = sb.build();
        sqlQuery.output();
    }

    @Test
    public void testApp() throws Exception {
        SqlBuilder sb = SqlBuilder.createQuery().table("TABLE1", "T1").end();

        // 拼装查询一张表的语句
        Sql sqlQuery = sb.build();
        Assert.assertEquals("SELECT * FROM TABLE1 AS T1", sqlQuery.output());
        Assert.assertEquals(0, sqlQuery.getParameters().size());

        // 拼装两张表关联查询
        sb.select().column("T1.*").end().table()
                .innerJoin(new Table("TABLE2", "T2"))
                .joinCondition("T2.ID", "T1.ID")
                .end();
        sqlQuery = sb.build();
        Assert.assertEquals("SELECT T1.* FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID)",
                sqlQuery.output());
        Assert.assertEquals(0, sqlQuery.getParameters().size());

        // 拼装两张表关联查询,带二元查询条件
        sb.condition().operator("=").left("T1.CATAGORY").right(new Value("TEST")).end();
        sqlQuery = sb.build();
        Assert.assertEquals("SELECT T1.* FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // 拼装两张表关联查询,带二元查询条件,增加排序
        sb.order().column("T1.CREATE_DATE").desc().end();
        sqlQuery = sb.build();

        Assert.assertEquals(
                "SELECT T1.* FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) ORDER BY T1.CREATE_DATE DESC",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // 去掉排序
        sb.removeOrder();
        sqlQuery = sb.build();
        Assert.assertEquals("SELECT T1.* FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));
        // MYSQL LIMIT查询
        sb.firstResults(100).maxResults(400);
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.* FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) LIMIT ?,?",
                sqlQuery.output());
        Assert.assertEquals(3, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));
        Assert.assertEquals(100, sqlQuery.getParameters().get(1));
        Assert.assertEquals(400, sqlQuery.getParameters().get(2));

        // 移除LIMIT
        sb.removeFirstResult().removeMaxResults();

        // COUNT查询
        sb.isCount();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT COUNT(*) COUNT FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // 去掉COUNT
        sb.isCount(false);
        sqlQuery = sb.build();
        Assert.assertEquals("SELECT T1.* FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // 多列查询
        sb.select().column("T2", "ID", "ID2").column("T2.CODE", "CODE2").end();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.*,T2.ID ID2,T2.CODE CODE2 FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // IS NULL查询条件
        sb.isNullCondition().column(new Column("T1.CREATOR")).end();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.*,T2.ID ID2,T2.CODE CODE2 FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) AND (T1.CREATOR IS NULL)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // IS NOT NULL查询条件
        sb.isNotNullCondition().column(new Column("T2.ENABLED")).end();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.*,T2.ID ID2,T2.CODE CODE2 FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) AND (T1.CREATOR IS NULL) AND (T2.ENABLED IS NOT NULL)",
                sqlQuery.output());
        Assert.assertEquals(1, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));

        // BETWEEN查询条件
        sb.betweenCondition().column("T1.START_DATE").leftValue(new Value(2015, Integer.class))
                .rightValue(new Value(2018, Integer.class)).end();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.*,T2.ID ID2,T2.CODE CODE2 FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) AND (T1.CREATOR IS NULL) AND (T2.ENABLED IS NOT NULL) AND (T1.START_DATE BETWEEN ? AND ?)",
                sqlQuery.output());
        Assert.assertEquals(3, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));
        Assert.assertEquals(2015, sqlQuery.getParameters().get(1));
        Assert.assertEquals(2018, sqlQuery.getParameters().get(2));

        // IN查询条件
        sb.inCondition().column("T2.CODE").value(new Value("1")).value(new Value("2")).value(new Value("3"))
                .value(new Value("4")).value(new Value("5")).value(new Value("6")).end();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.*,T2.ID ID2,T2.CODE CODE2 FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) AND (T1.CREATOR IS NULL) AND (T2.ENABLED IS NOT NULL) AND (T1.START_DATE BETWEEN ? AND ?) AND (T2.CODE IN(?,?,?,?,?,?))",
                sqlQuery.output());
        Assert.assertEquals(9, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));
        Assert.assertEquals(2015, sqlQuery.getParameters().get(1));
        Assert.assertEquals(2018, sqlQuery.getParameters().get(2));
        Assert.assertEquals("1", sqlQuery.getParameters().get(3));
        Assert.assertEquals("2", sqlQuery.getParameters().get(4));
        Assert.assertEquals("3", sqlQuery.getParameters().get(5));
        Assert.assertEquals("4", sqlQuery.getParameters().get(6));
        Assert.assertEquals("5", sqlQuery.getParameters().get(7));
        Assert.assertEquals("6", sqlQuery.getParameters().get(8));

        // 大于1000个值的IN语句
        List<IValue> values = new ArrayList<>();
        for (int i = 0; i < 2235; i++) {
            values.add(new Value("test" + i));
        }

        sb.inCondition().column(new Column("T1.NAME")).value(values).end();
        sqlQuery = sb.build();
        Assert.assertEquals(
                "SELECT T1.*,T2.ID ID2,T2.CODE CODE2 FROM TABLE1 AS T1 INNER JOIN TABLE2 AS T2 ON (T2.ID = T1.ID) WHERE (T1.CATAGORY = ?) AND (T1.CREATOR IS NULL) AND (T2.ENABLED IS NOT NULL) AND (T1.START_DATE BETWEEN ? AND ?) AND (T2.CODE IN(?,?,?,?,?,?)) AND (T1.NAME IN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) OR T1.NAME IN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?) OR T1.NAME IN(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?))",
                sqlQuery.output());
        Assert.assertEquals(2244, sqlQuery.getParameters().size());
        Assert.assertEquals("TEST", sqlQuery.getParameters().get(0));
        Assert.assertEquals(2015, sqlQuery.getParameters().get(1));
        Assert.assertEquals(2018, sqlQuery.getParameters().get(2));
        Assert.assertEquals("1", sqlQuery.getParameters().get(3));
        Assert.assertEquals("2", sqlQuery.getParameters().get(4));
        Assert.assertEquals("3", sqlQuery.getParameters().get(5));
        Assert.assertEquals("4", sqlQuery.getParameters().get(6));
        Assert.assertEquals("5", sqlQuery.getParameters().get(7));
        Assert.assertEquals("6", sqlQuery.getParameters().get(8));

        for (int i = 0; i < 2235; i++) {
            Assert.assertEquals("test" + i, sqlQuery.getParameters().get(i + 9));
        }
    }
}
