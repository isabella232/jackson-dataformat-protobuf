package com.fasterxml.jackson.dataformat.protobuf.schema;

import java.util.*;

import com.fasterxml.jackson.core.SerializableString;
import com.squareup.protoparser.MessageType.Field;

public class ProtobufField
// sorted in increasing order
    implements Comparable<ProtobufField>
{
    /**
     * Numeric tag, unshifted
     */
    public final int id;

    /**
     * Combination of numeric tag and 3-bit wire type.
     */
    public final int typedTag;
    
    /**
     * Name of field in protoc definition
     */
    public final String name;

    public final FieldType type;

    /**
     * 3-bit id used on determining details of how values are serialized.
     */
    public final int wireType;
    
    public final boolean required, repeated, packed, deprecated;
    public final boolean usesZigZag;

    /**
     * For main type of {@link FieldType#MESSAGE}, reference to actual
     * message type definition.
     */
    protected ProtobufMessage messageType;

    /**
     * For fields of type {@link FieldType#ENUM}, mapping from names to ids.
     */
    protected final EnumLookup enumValues;

    /**
     * Link to next field within message definition; used for efficient traversal.
     * Due to inverse construction order need to be assigned after construction;
     * but functionally immutable.
     */
    public ProtobufField next;
    
    public final boolean isObject;

    public final boolean isStdEnum;
    
    public ProtobufField(Field nativeField, FieldType type) {
        this(nativeField, type, null, null);
    }

    public ProtobufField(Field nativeField, ProtobufMessage msg) {
        this(nativeField, FieldType.MESSAGE, msg, null);
    }

    public ProtobufField(Field nativeField, ProtobufEnum et) {
        this(nativeField, FieldType.ENUM, null, et);
    }

    public static ProtobufField unknownField() {
        return new ProtobufField(null, FieldType.MESSAGE, null, null);
    }
    
    protected ProtobufField(Field nativeField, FieldType type,
            ProtobufMessage msg, ProtobufEnum et)
    {
        this.type = type;
        wireType = type.getWireType();
        usesZigZag = type.usesZigZag();
        if (et == null) {
            enumValues = EnumLookup.empty();
            isStdEnum = false;
        } else {
            enumValues = EnumLookup.construct(et);
            isStdEnum = et.usesStandardIndexing();
        }
        messageType = msg;

        if (nativeField == null) { // for "unknown" field
            typedTag = id = 0;
            repeated = required = deprecated = packed = false;
            name = "UNKNOWN";
        } else {
            id = nativeField.getTag();
            typedTag = (id << 3) + wireType;
            name = nativeField.getName();
            switch (nativeField.getLabel()) {
            case REPEATED:
                required = false;
                repeated = true;
                break;
            case REQUIRED:
                required = true;
                repeated = false;
                break;
            default:
                required = repeated = false;
                break;
            }
            packed = nativeField.isPacked();
            deprecated = nativeField.isDeprecated();
        }
        isObject = (type == FieldType.MESSAGE);
    }

    public void assignMessageType(ProtobufMessage msgType) {
        if (type != FieldType.MESSAGE) {
            throw new IllegalStateException("Can not assign message type for non-message field '"+name+"'");
        }
        messageType = msgType;
    }

    public void assignNext(ProtobufField n) {
        if (this.next != null) {
            throw new IllegalStateException("Can not overwrite 'next' after being set");
        }
        this.next = n;
    }

    public final ProtobufMessage getMessageType() {
        return messageType;
    }

    public final ProtobufField nextOrThisIf(int idToMatch) {
        if ((next != null) && (next.id == idToMatch)) {
            return next;
        }
        // or maybe we actually have the id?
        if (idToMatch == id) {
            return this;
        }
        return null;
    }

    public final ProtobufField nextIf(String nameToMatch) {
        if (next != null) {
            if ((nameToMatch == next.name) || nameToMatch.equals(next.name)) {
                return next;
            }
        }
        return null;
    }

    public final int findEnumIndex(SerializableString key) {
        return enumValues.findEnumIndex(key);
    }

    public final int findEnumIndex(String key) {
        return enumValues.findEnumIndex(key);
    }
    public final String findEnumByIndex(int index) {
        return enumValues.findEnumByIndex(index);
    }

    public Collection<String> getEnumValues() {
        return enumValues.getEnumValues();
    }

    public final boolean isArray() {
        return repeated;
    }

    public final boolean isValidFor(int typeTag) {
        return (typeTag == type.getWireType());
    }

    @Override
    public String toString() // for debugging
    {
        return "Field '"+name+"', tag="+typedTag+", wireType="+wireType+", fieldType="+type;
    }

    @Override
    public int compareTo(ProtobufField other) {
        return id - other.id;
    }
}
