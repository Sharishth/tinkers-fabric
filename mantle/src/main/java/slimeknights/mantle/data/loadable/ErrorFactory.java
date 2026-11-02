package slimeknights.mantle.data.loadable;

import com.google.gson.JsonSyntaxException;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import slimeknights.mantle.data.loadable.field.ConstantField;
import slimeknights.mantle.data.loadable.field.RecordField;

import java.util.function.Consumer;

/**
 * Creates the exception type that fits where a loadable is running.
 * Mapping functions receive one, so the same validation throws a JSON error while parsing and a decoder error on the network.
 */
public interface ErrorFactory extends Consumer<String> {
  /** Errors while reading JSON */
  ErrorFactory JSON_SYNTAX_ERROR = JsonSyntaxException::new;
  /** Errors while reading the network */
  ErrorFactory DECODER_EXCEPTION = DecoderException::new;
  /** Errors while writing the network */
  ErrorFactory ENCODER_EXCEPTION = EncoderException::new;
  /** Errors while writing JSON; keeps runtime exceptions as they are */
  ErrorFactory RUNTIME = new ErrorFactory() {
    @Override
    public RuntimeException create(String error) {
      return new RuntimeException(error);
    }

    @Override
    public RuntimeException create(RuntimeException base) {
      return base;
    }
  };
  /** Record field giving a constructor the right factory, so it can throw while parsing JSON or the network */
  RecordField<ErrorFactory, Object> FIELD = new ConstantField<>(JSON_SYNTAX_ERROR, DECODER_EXCEPTION);

  /** Creates an exception with the given message */
  RuntimeException create(String error);

  /** Creates an exception carrying the message of the given exception */
  default RuntimeException create(RuntimeException base) {
    return create(base.getMessage());
  }

  /** Throws an exception with the given message */
  @Override
  default void accept(String error) {
    throw create(error);
  }
}
