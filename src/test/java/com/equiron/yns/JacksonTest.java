package com.equiron.yns;

import java.io.IOException;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.type.TypeFactory;

class Document {/*empty*/}

class Row {/*empty*/}

class RowWithDoc<D extends Document> extends Row {
    @JsonProperty("d")
    D d;
}

class ResultSet<R extends Row> {
    @JsonProperty("rows")
    List<R> rows;
}

class ResultSetWithDoc<D extends Document> extends ResultSet<RowWithDoc<D>> {/*empty*/}

class MyDoc extends Document {/*empty*/}

public class JacksonTest {
    @Test
    public void shouldWorksWithGenerics() throws IOException {
        String json = "{\"rows\":[{\"d\":{}}]}";

        JavaType jt = TypeFactory.defaultInstance().constructType(new TypeReference<ResultSetWithDoc<MyDoc>>() {/*empty*/});

        ResultSetWithDoc<MyDoc> rs = new ObjectMapper().readValue(json, jt);

        Document d = rs.rows.iterator().next().d;

        Assertions.assertEquals(MyDoc.class, d.getClass());
    }
}
