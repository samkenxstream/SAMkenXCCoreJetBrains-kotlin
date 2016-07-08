// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: location.proto

package proto.car;

public final class LocationP {
  private LocationP() {}
  public static void registerAllExtensions(
      com.google.protobuf.ExtensionRegistry registry) {
  }
  public interface LocationOrBuilder extends
      // @@protoc_insertion_point(interface_extends:carkot.Location)
      com.google.protobuf.MessageOrBuilder {

    /**
     * <code>optional double x = 1;</code>
     */
    double getX();

    /**
     * <code>optional double y = 2;</code>
     */
    double getY();

    /**
     * <code>optional double angle = 3;</code>
     */
    double getAngle();
  }
  /**
   * Protobuf type {@code carkot.Location}
   */
  public  static final class Location extends
      com.google.protobuf.GeneratedMessage implements
      // @@protoc_insertion_point(message_implements:carkot.Location)
      LocationOrBuilder {
    // Use Location.newBuilder() to construct.
    private Location(com.google.protobuf.GeneratedMessage.Builder<?> builder) {
      super(builder);
    }
    private Location() {
      x_ = 0D;
      y_ = 0D;
      angle_ = 0D;
    }

    @java.lang.Override
    public final com.google.protobuf.UnknownFieldSet
    getUnknownFields() {
      return com.google.protobuf.UnknownFieldSet.getDefaultInstance();
    }
    private Location(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      this();
      int mutable_bitField0_ = 0;
      try {
        boolean done = false;
        while (!done) {
          int tag = input.readTag();
          switch (tag) {
            case 0:
              done = true;
              break;
            default: {
              if (!input.skipField(tag)) {
                done = true;
              }
              break;
            }
            case 9: {

              x_ = input.readDouble();
              break;
            }
            case 17: {

              y_ = input.readDouble();
              break;
            }
            case 25: {

              angle_ = input.readDouble();
              break;
            }
          }
        }
      } catch (com.google.protobuf.InvalidProtocolBufferException e) {
        throw e.setUnfinishedMessage(this);
      } catch (java.io.IOException e) {
        throw new com.google.protobuf.InvalidProtocolBufferException(
            e).setUnfinishedMessage(this);
      } finally {
        makeExtensionsImmutable();
      }
    }
    public static final com.google.protobuf.Descriptors.Descriptor
        getDescriptor() {
      return proto.car.LocationP.internal_static_carkot_Location_descriptor;
    }

    protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
        internalGetFieldAccessorTable() {
      return proto.car.LocationP.internal_static_carkot_Location_fieldAccessorTable
          .ensureFieldAccessorsInitialized(
              proto.car.LocationP.Location.class, proto.car.LocationP.Location.Builder.class);
    }

    public static final int X_FIELD_NUMBER = 1;
    private double x_;
    /**
     * <code>optional double x = 1;</code>
     */
    public double getX() {
      return x_;
    }

    public static final int Y_FIELD_NUMBER = 2;
    private double y_;
    /**
     * <code>optional double y = 2;</code>
     */
    public double getY() {
      return y_;
    }

    public static final int ANGLE_FIELD_NUMBER = 3;
    private double angle_;
    /**
     * <code>optional double angle = 3;</code>
     */
    public double getAngle() {
      return angle_;
    }

    private byte memoizedIsInitialized = -1;
    public final boolean isInitialized() {
      byte isInitialized = memoizedIsInitialized;
      if (isInitialized == 1) return true;
      if (isInitialized == 0) return false;

      memoizedIsInitialized = 1;
      return true;
    }

    public void writeTo(com.google.protobuf.CodedOutputStream output)
                        throws java.io.IOException {
      if (x_ != 0D) {
        output.writeDouble(1, x_);
      }
      if (y_ != 0D) {
        output.writeDouble(2, y_);
      }
      if (angle_ != 0D) {
        output.writeDouble(3, angle_);
      }
    }

    public int getSerializedSize() {
      int size = memoizedSize;
      if (size != -1) return size;

      size = 0;
      if (x_ != 0D) {
        size += com.google.protobuf.CodedOutputStream
          .computeDoubleSize(1, x_);
      }
      if (y_ != 0D) {
        size += com.google.protobuf.CodedOutputStream
          .computeDoubleSize(2, y_);
      }
      if (angle_ != 0D) {
        size += com.google.protobuf.CodedOutputStream
          .computeDoubleSize(3, angle_);
      }
      memoizedSize = size;
      return size;
    }

    private static final long serialVersionUID = 0L;
    public static proto.car.LocationP.Location parseFrom(
        com.google.protobuf.ByteString data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static proto.car.LocationP.Location parseFrom(
        com.google.protobuf.ByteString data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static proto.car.LocationP.Location parseFrom(byte[] data)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data);
    }
    public static proto.car.LocationP.Location parseFrom(
        byte[] data,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws com.google.protobuf.InvalidProtocolBufferException {
      return PARSER.parseFrom(data, extensionRegistry);
    }
    public static proto.car.LocationP.Location parseFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static proto.car.LocationP.Location parseFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }
    public static proto.car.LocationP.Location parseDelimitedFrom(java.io.InputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input);
    }
    public static proto.car.LocationP.Location parseDelimitedFrom(
        java.io.InputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseDelimitedWithIOException(PARSER, input, extensionRegistry);
    }
    public static proto.car.LocationP.Location parseFrom(
        com.google.protobuf.CodedInputStream input)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input);
    }
    public static proto.car.LocationP.Location parseFrom(
        com.google.protobuf.CodedInputStream input,
        com.google.protobuf.ExtensionRegistryLite extensionRegistry)
        throws java.io.IOException {
      return com.google.protobuf.GeneratedMessage
          .parseWithIOException(PARSER, input, extensionRegistry);
    }

    public Builder newBuilderForType() { return newBuilder(); }
    public static Builder newBuilder() {
      return DEFAULT_INSTANCE.toBuilder();
    }
    public static Builder newBuilder(proto.car.LocationP.Location prototype) {
      return DEFAULT_INSTANCE.toBuilder().mergeFrom(prototype);
    }
    public Builder toBuilder() {
      return this == DEFAULT_INSTANCE
          ? new Builder() : new Builder().mergeFrom(this);
    }

    @java.lang.Override
    protected Builder newBuilderForType(
        com.google.protobuf.GeneratedMessage.BuilderParent parent) {
      Builder builder = new Builder(parent);
      return builder;
    }
    /**
     * Protobuf type {@code carkot.Location}
     */
    public static final class Builder extends
        com.google.protobuf.GeneratedMessage.Builder<Builder> implements
        // @@protoc_insertion_point(builder_implements:carkot.Location)
        proto.car.LocationP.LocationOrBuilder {
      public static final com.google.protobuf.Descriptors.Descriptor
          getDescriptor() {
        return proto.car.LocationP.internal_static_carkot_Location_descriptor;
      }

      protected com.google.protobuf.GeneratedMessage.FieldAccessorTable
          internalGetFieldAccessorTable() {
        return proto.car.LocationP.internal_static_carkot_Location_fieldAccessorTable
            .ensureFieldAccessorsInitialized(
                proto.car.LocationP.Location.class, proto.car.LocationP.Location.Builder.class);
      }

      // Construct using proto.car.LocationP.Location.newBuilder()
      private Builder() {
        maybeForceBuilderInitialization();
      }

      private Builder(
          com.google.protobuf.GeneratedMessage.BuilderParent parent) {
        super(parent);
        maybeForceBuilderInitialization();
      }
      private void maybeForceBuilderInitialization() {
        if (com.google.protobuf.GeneratedMessage.alwaysUseFieldBuilders) {
        }
      }
      public Builder clear() {
        super.clear();
        x_ = 0D;

        y_ = 0D;

        angle_ = 0D;

        return this;
      }

      public com.google.protobuf.Descriptors.Descriptor
          getDescriptorForType() {
        return proto.car.LocationP.internal_static_carkot_Location_descriptor;
      }

      public proto.car.LocationP.Location getDefaultInstanceForType() {
        return proto.car.LocationP.Location.getDefaultInstance();
      }

      public proto.car.LocationP.Location build() {
        proto.car.LocationP.Location result = buildPartial();
        if (!result.isInitialized()) {
          throw newUninitializedMessageException(result);
        }
        return result;
      }

      public proto.car.LocationP.Location buildPartial() {
        proto.car.LocationP.Location result = new proto.car.LocationP.Location(this);
        result.x_ = x_;
        result.y_ = y_;
        result.angle_ = angle_;
        onBuilt();
        return result;
      }

      public Builder mergeFrom(com.google.protobuf.Message other) {
        if (other instanceof proto.car.LocationP.Location) {
          return mergeFrom((proto.car.LocationP.Location)other);
        } else {
          super.mergeFrom(other);
          return this;
        }
      }

      public Builder mergeFrom(proto.car.LocationP.Location other) {
        if (other == proto.car.LocationP.Location.getDefaultInstance()) return this;
        if (other.getX() != 0D) {
          setX(other.getX());
        }
        if (other.getY() != 0D) {
          setY(other.getY());
        }
        if (other.getAngle() != 0D) {
          setAngle(other.getAngle());
        }
        onChanged();
        return this;
      }

      public final boolean isInitialized() {
        return true;
      }

      public Builder mergeFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws java.io.IOException {
        proto.car.LocationP.Location parsedMessage = null;
        try {
          parsedMessage = PARSER.parsePartialFrom(input, extensionRegistry);
        } catch (com.google.protobuf.InvalidProtocolBufferException e) {
          parsedMessage = (proto.car.LocationP.Location) e.getUnfinishedMessage();
          throw e.unwrapIOException();
        } finally {
          if (parsedMessage != null) {
            mergeFrom(parsedMessage);
          }
        }
        return this;
      }

      private double x_ ;
      /**
       * <code>optional double x = 1;</code>
       */
      public double getX() {
        return x_;
      }
      /**
       * <code>optional double x = 1;</code>
       */
      public Builder setX(double value) {
        
        x_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional double x = 1;</code>
       */
      public Builder clearX() {
        
        x_ = 0D;
        onChanged();
        return this;
      }

      private double y_ ;
      /**
       * <code>optional double y = 2;</code>
       */
      public double getY() {
        return y_;
      }
      /**
       * <code>optional double y = 2;</code>
       */
      public Builder setY(double value) {
        
        y_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional double y = 2;</code>
       */
      public Builder clearY() {
        
        y_ = 0D;
        onChanged();
        return this;
      }

      private double angle_ ;
      /**
       * <code>optional double angle = 3;</code>
       */
      public double getAngle() {
        return angle_;
      }
      /**
       * <code>optional double angle = 3;</code>
       */
      public Builder setAngle(double value) {
        
        angle_ = value;
        onChanged();
        return this;
      }
      /**
       * <code>optional double angle = 3;</code>
       */
      public Builder clearAngle() {
        
        angle_ = 0D;
        onChanged();
        return this;
      }
      public final Builder setUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }

      public final Builder mergeUnknownFields(
          final com.google.protobuf.UnknownFieldSet unknownFields) {
        return this;
      }


      // @@protoc_insertion_point(builder_scope:carkot.Location)
    }

    // @@protoc_insertion_point(class_scope:carkot.Location)
    private static final proto.car.LocationP.Location DEFAULT_INSTANCE;
    static {
      DEFAULT_INSTANCE = new proto.car.LocationP.Location();
    }

    public static proto.car.LocationP.Location getDefaultInstance() {
      return DEFAULT_INSTANCE;
    }

    private static final com.google.protobuf.Parser<Location>
        PARSER = new com.google.protobuf.AbstractParser<Location>() {
      public Location parsePartialFrom(
          com.google.protobuf.CodedInputStream input,
          com.google.protobuf.ExtensionRegistryLite extensionRegistry)
          throws com.google.protobuf.InvalidProtocolBufferException {
          return new Location(input, extensionRegistry);
      }
    };

    public static com.google.protobuf.Parser<Location> parser() {
      return PARSER;
    }

    @java.lang.Override
    public com.google.protobuf.Parser<Location> getParserForType() {
      return PARSER;
    }

    public proto.car.LocationP.Location getDefaultInstanceForType() {
      return DEFAULT_INSTANCE;
    }

  }

  private static final com.google.protobuf.Descriptors.Descriptor
    internal_static_carkot_Location_descriptor;
  private static final 
    com.google.protobuf.GeneratedMessage.FieldAccessorTable
      internal_static_carkot_Location_fieldAccessorTable;

  public static com.google.protobuf.Descriptors.FileDescriptor
      getDescriptor() {
    return descriptor;
  }
  private static  com.google.protobuf.Descriptors.FileDescriptor
      descriptor;
  static {
    java.lang.String[] descriptorData = {
      "\n\016location.proto\022\006carkot\"/\n\010Location\022\t\n\001" +
      "x\030\001 \001(\001\022\t\n\001y\030\002 \001(\001\022\r\n\005angle\030\003 \001(\001B\026\n\tpro" +
      "to.carB\tLocationPb\006proto3"
    };
    com.google.protobuf.Descriptors.FileDescriptor.InternalDescriptorAssigner assigner =
        new com.google.protobuf.Descriptors.FileDescriptor.    InternalDescriptorAssigner() {
          public com.google.protobuf.ExtensionRegistry assignDescriptors(
              com.google.protobuf.Descriptors.FileDescriptor root) {
            descriptor = root;
            return null;
          }
        };
    com.google.protobuf.Descriptors.FileDescriptor
      .internalBuildGeneratedFileFrom(descriptorData,
        new com.google.protobuf.Descriptors.FileDescriptor[] {
        }, assigner);
    internal_static_carkot_Location_descriptor =
      getDescriptor().getMessageTypes().get(0);
    internal_static_carkot_Location_fieldAccessorTable = new
      com.google.protobuf.GeneratedMessage.FieldAccessorTable(
        internal_static_carkot_Location_descriptor,
        new java.lang.String[] { "X", "Y", "Angle", });
  }

  // @@protoc_insertion_point(outer_class_scope)
}
