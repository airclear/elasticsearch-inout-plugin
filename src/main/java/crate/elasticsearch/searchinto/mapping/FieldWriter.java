package crate.elasticsearch.searchinto.mapping;

import java.util.HashMap;
import java.util.Map;

import org.elasticsearch.ElasticSearchException;
import org.elasticsearch.common.collect.ImmutableMap;
import org.elasticsearch.common.collect.MapBuilder;
import org.elasticsearch.index.mapper.internal.IdFieldMapper;
import org.elasticsearch.index.mapper.internal.IndexFieldMapper;
import org.elasticsearch.index.mapper.internal.SourceFieldMapper;
import org.elasticsearch.index.mapper.internal.TTLFieldMapper;
import org.elasticsearch.index.mapper.internal.TimestampFieldMapper;
import org.elasticsearch.index.mapper.internal.TypeFieldMapper;

public class FieldWriter {

    private final String name;
    protected Object value;
    private BuilderWriter writer;

    private final static ImmutableMap<String, BuilderWriter> writers;

    static {
        writers = MapBuilder.<String, BuilderWriter>newMapBuilder().put(
                SourceFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.source = (Map<String, Object>) value;
            }
        }).put(IndexFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.index(value.toString());
            }
        }).put(IdFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.id(value.toString());
            }
        }).put(TypeFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.type(value.toString());
            }
        }).put(TimestampFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.timestamp(value.toString());
            }
        }).put(TTLFieldMapper.NAME, new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.ttl((Long) value);
            }
        }).put("_version", new BuilderWriter() {
            @Override
            public void write(IndexRequestBuilder builder, Object value) {
                builder.request.version((Long) value);
            }
        }).immutableMap();
    }

    static abstract class BuilderWriter {
        public abstract void write(IndexRequestBuilder builder, Object value);
    }

    class SourceObjectWriter extends BuilderWriter {

        private final String name;

        SourceObjectWriter(String name) {
            this.name = name;
        }

        private Map<String, Object> writeToMap(Object root, Object value, String part) {
            if (root == null) {
                root = new HashMap<String, Object>();
            } else if (!(root instanceof Map)) {
                throw new ElasticSearchException("Error on rewriting objects: Mixed objects and values");
            }
            if (part.contains(".")) {
                String[] parts = part.split("\\.", 2);
                ((Map<String, Object>) root).put(parts[0], writeToMap(((Map<String, Object>) root).get(parts[0]), value, parts[1]));
            } else {
                if (((Map<String, Object>) root).get(part) instanceof Map) {
                    throw new ElasticSearchException("Error on rewriting objects: Mixed objects and values");
                }
                ((Map<String, Object>)root).put(part, value);
            }
            return (Map<String, Object>) root;
        }

        @Override
        public void write(IndexRequestBuilder builder, Object value) {
            if (value != null) {
                if (name.contains(".")) {
                    String[] list = name.split("\\.", 2);
                    builder.source.put(list[0], writeToMap(builder.source.get(list[0]), value, list[1]));
                } else {
                    if (builder.source.get(name) instanceof Map) {
                        throw new ElasticSearchException("Error on rewriting objects: Mixed objects and values");
                    }
                    builder.source.put(name, value);
                }
            }
        }
    }

    public FieldWriter(String name) {
        this.name = name;
        initWriter();
    }

    private void initWriter() {
        if (name.startsWith("_")) {
            writer = writers.get(name);
        }
        if (writer == null) {
            writer = new SourceObjectWriter(name);
        }
    }

    public void setValue(Object value) {
        this.value = value;
    }

    public IndexRequestBuilder toRequestBuilder(IndexRequestBuilder builder) {
        writer.write(builder, value);
        return builder;
    }


}
