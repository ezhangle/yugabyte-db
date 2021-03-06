// Copyright (c) YugaByte, Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
// in compliance with the License.  You may obtain a copy of the License at
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software distributed under the License
// is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
// or implied.  See the License for the specific language governing permissions and limitations
// under the License.
//

package org.yb.pgsql;

import org.junit.Test;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.HashSet;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.yb.AssertionWrappers.assertEquals;
import static org.yb.AssertionWrappers.assertFalse;
import static org.yb.AssertionWrappers.assertTrue;

import org.junit.runner.RunWith;

import org.yb.YBTestRunner;

@RunWith(value=YBTestRunner.class)
public class TestPgInsert extends BasePgSQLTest {
  private static final Logger LOG = LoggerFactory.getLogger(TestPgInsert.class);

  @Test
  public void testBasicInsert() throws SQLException {
    createSimpleTable("test");
    try (Statement statement = connection.createStatement()) {
      Set<Row> expectedRows = new HashSet<>();

      // Test simple insert.
      statement.execute("INSERT INTO test(h, r, vi, vs) VALUES (1, 1.5, 2, 'abc')");
      expectedRows.add(new Row(1L, 1.5D, 2, "abc"));

      // Test different column order.
      statement.execute("INSERT INTO test(vs, vi, h, r) VALUES ('def', 3, 2, 2.5)");
      expectedRows.add(new Row(2L, 2.5D, 3, "def"));

      // Test null values (for non-primary-key columns).
      statement.execute("INSERT INTO test(h, r, vi, vs) VALUES (3, 3.5, null, null)");
      // 0 and "" are the default values JDBC will report for integer and text types, respectively.
      expectedRows.add(new Row(3L, 3.5D, null, null));

      // Test missing values (for non-primary-key columns).
      statement.execute("INSERT INTO test(h, r) VALUES (4, 4.5)");
      // 0 and "" are the default values JDBC will report for integer and text types, respectively.
      expectedRows.add(new Row(4L, 4.5D, null, null));

      // Test inserting multiple value sets.
      statement.execute("INSERT INTO test(h, r, vi, vs) VALUES (5, 5.5, 6, 'ghi'), " +
                            "(6, 6.5, 7, 'jkl'), (7, 7.5, 8, 'mno')");
      expectedRows.add(new Row(5L, 5.5D, 6, "ghi"));
      expectedRows.add(new Row(6L, 6.5D, 7, "jkl"));
      expectedRows.add(new Row(7L, 7.5D, 8, "mno"));

      // Test null values (for primary-key columns) -- expecting error.
      runInvalidQuery(statement, "INSERT INTO test(h, r, vi, vs) VALUES (8, null, 2, 'abc')");
      runInvalidQuery(statement, "INSERT INTO test(h, r, vi, vs) VALUES (null, 8.5, 2, 'abc')");

      // Test missing values (for primary-key columns) -- expecting error.
      runInvalidQuery(statement, "INSERT INTO test(r, vi, vs) VALUES (9, 2, 'abc')");
      runInvalidQuery(statement, "INSERT INTO test(h, vi, vs) VALUES (9.5, 2, 'abc')");

      // Test duplicate primary key -- expecting error;
      runInvalidQuery(statement, "INSERT INTO test(h, r, vi, vs) VALUES (1, 1.5, 2, 'abc')");
      runInvalidQuery(statement, "INSERT INTO test(h, r, vi, vs) VALUES (1, 1.5, 9, 'xyz')");
      runInvalidQuery(statement, "INSERT INTO test(h, r) VALUES (1, 1.5)");

      // Check rows.
      try (ResultSet rs = statement.executeQuery("SELECT * FROM test")) {
        assertEquals(expectedRows, getRowSet(rs));
      }
    }
  }

  @Test
  public void testExpressions() throws Exception {
    try (Statement statement = connection.createStatement()) {
      createSimpleTable("test");

      // Test expressions in INSERT values.
      statement.execute("INSERT INTO test(h, r, vi, vs) " +
                            "VALUES (floor(1 + 1.5), log(3, 27), ceil(pi()), 'ab' || 'c')");
      assertOneRow("SELECT * FROM test", 2L, 3.0D, 4, "abc");
    }
  }

}
