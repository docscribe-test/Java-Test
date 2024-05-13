package redis.clients.jedis.search;

import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.DIALECT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.EXPANDER;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.FIELDS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.FILTER;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.FRAGS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.GEOFILTER;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.HIGHLIGHT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.INFIELDS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.INKEYS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.INORDER;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.LANGUAGE;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.LEN;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.LIMIT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NOCONTENT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.NOSTOPWORDS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.PARAMS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.RETURN;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SCORER;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SEPARATOR;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SLOP;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SORTBY;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.SUMMARIZE;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.TAGS;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.TIMEOUT;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.VERBATIM;
import static redis.clients.jedis.search.SearchProtocol.SearchKeyword.WITHSCORES;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import redis.clients.jedis.CommandArguments;
import redis.clients.jedis.Protocol;
import redis.clients.jedis.annots.Internal;
import redis.clients.jedis.args.GeoUnit;
import redis.clients.jedis.args.SortingOrder;
import redis.clients.jedis.params.IParams;
import redis.clients.jedis.util.LazyRawable;

/**
* This class is used to set the parameters for a full text search query.
*/
public class FTSearchParams implements IParams {

  private boolean noContent = false;
  private boolean verbatim = false;
  private boolean noStopwords = false;
  private boolean withScores = false;
  private final List<IParams> filters = new LinkedList<>();
  private Collection<String> inKeys;
  private Collection<String> inFields;
  private Collection<String> returnFields;
  private Collection<FieldName> returnFieldNames;
  private boolean summarize;
  private SummarizeParams summarizeParams;
  private boolean highlight;
  private HighlightParams highlightParams;
  private Integer slop;
  private Long timeout;
  private boolean inOrder;
  private String language;
  private String expander;
  private String scorer;
  // private boolean explainScore; // TODO
  private String sortBy;
  private SortingOrder sortOrder;
  private int[] limit;
  private Map<String, Object> params;
  private Integer dialect;

  /**
  * The default constructor for FTSearchParams.
  */
  public FTSearchParams() {
  }

  /**
  * This method creates a new instance of FTSearchParams.
  * @return A new instance of FTSearchParams.
  */
  public static FTSearchParams searchParams() {
    return new FTSearchParams();
  }

  @Override
  /**
  * This method adds parameters to the search query.
  * @param args The command arguments to be added.
  */
  public void addParams(CommandArguments args) {

    if (noContent) {
      args.add(NOCONTENT);
    }
    if (verbatim) {
      args.add(VERBATIM);
    }
    if (noStopwords) {
      args.add(NOSTOPWORDS);
    }
    if (withScores) {
      args.add(WITHSCORES);
    }

    if (!filters.isEmpty()) {
      filters.forEach(filter -> filter.addParams(args));
    }

    if (inKeys != null && !inKeys.isEmpty()) {
      args.add(INKEYS).add(inKeys.size()).addObjects(inKeys);
    }

    if (inFields != null && !inFields.isEmpty()) {
      args.add(INFIELDS).add(inFields.size()).addObjects(inFields);
    }

    if (returnFieldNames != null && !returnFieldNames.isEmpty()) {
      args.add(RETURN);
      LazyRawable returnCountObject = new LazyRawable();
      args.add(returnCountObject); // holding a place for setting the total count later.
      int returnCount = 0;
      for (FieldName fn : returnFieldNames) {
        returnCount += fn.addCommandArguments(args);
      }
      returnCountObject.setRaw(Protocol.toByteArray(returnCount));
    } else if (returnFields != null && !returnFields.isEmpty()) {
      args.add(RETURN).add(returnFields.size()).addObjects(returnFields);
    }

    if (summarizeParams != null) {
      args.addParams(summarizeParams);
    } else if (summarize) {
      args.add(SUMMARIZE);
    }

    if (highlightParams != null) {
      args.addParams(highlightParams);
    } else if (highlight) {
      args.add(HIGHLIGHT);
    }

    if (slop != null) {
      args.add(SLOP).add(slop);
    }

    if (timeout != null) {
      args.add(TIMEOUT).add(timeout);
    }

    if (inOrder) {
      args.add(INORDER);
    }

    if (language != null) {
      args.add(LANGUAGE).add(language);
    }

    if (expander != null) {
      args.add(EXPANDER).add(expander);
    }

    if (scorer != null) {
      args.add(SCORER).add(scorer);
    }
//
//    if (explainScore) {
//      args.add(EXPLAINSCORE);
//    }

    if (sortBy != null) {
      args.add(SORTBY).add(sortBy);
      if (sortOrder != null) {
        args.add(sortOrder);
      }
    }

    if (limit != null) {
      args.add(LIMIT).add(limit[0]).add(limit[1]);
    }

    if (params != null && !params.isEmpty()) {
      args.add(PARAMS).add(params.size() * 2);
      params.entrySet().forEach(entry -> args.add(entry.getKey()).add(entry.getValue()));
    }

    if (dialect != null) {
      args.add(DIALECT).add(dialect);
    }
  }

  /**
  * This method sets the noContent flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams noContent() {
    this.noContent = true;
    return this;
  }

  /**
  * This method sets the verbatim flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams verbatim() {
    this.verbatim = true;
    return this;
  }

  /**
  * This method sets the noStopwords flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams noStopwords() {
    this.noStopwords = true;
    return this;
  }

  /**
  * This method sets the withScores flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams withScores() {
    this.withScores = true;
    return this;
  }

  /**
  * This method creates a new numeric filter and adds it to the list of filters.
  * @param field The field for the new numeric filter.
  * @param min The minimum value for the new numeric filter.
  * @param max The maximum value for the new numeric filter.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams filter(String field, double min, double max) {
    return filter(new NumericFilter(field, min, max));
  }

  /**
  * This method creates a new numeric filter with exclusive min and max values and adds it to the list of filters.
  * @param field The field for the new numeric filter.
  * @param min The minimum value for the new numeric filter.
  * @param exclusiveMin The flag for exclusive minimum value.
  * @param max The maximum value for the new numeric filter.
  * @param exclusiveMax The flag for exclusive maximum value.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams filter(String field, double min, boolean exclusiveMin, double max, boolean exclusiveMax) {
    return filter(new NumericFilter(field, min, exclusiveMin, max, exclusiveMax));
  }

  /**
  * This method adds a numeric filter to the list of filters.
  * @param numericFilter The numeric filter to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams filter(NumericFilter numericFilter) {
    filters.add(numericFilter);
    return this;
  }

  /**
  * This method creates a new geo filter and adds it to the list of filters.
  * @param field The field for the new geo filter.
  * @param lon The longitude for the new geo filter.
  * @param lat The latitude for the new geo filter.
  * @param radius The radius for the new geo filter.
  * @param unit The unit for the new geo filter.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams geoFilter(String field, double lon, double lat, double radius, GeoUnit unit) {
    return geoFilter(new GeoFilter(field, lon, lat, radius, unit));
  }

  /**
  * This method adds a geo filter to the list of filters.
  * @param geoFilter The geo filter to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams geoFilter(GeoFilter geoFilter) {
    filters.add(geoFilter);
    return this;
  }

  /**
  * This method sets the inKeys collection to the keys passed as arguments.
  * @param keys The keys to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams inKeys(String... keys) {
    return inKeys(Arrays.asList(keys));
  }

  /**
  * This method sets the inKeys collection to a given collection of keys.
  * @param keys The collection of keys to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams inKeys(Collection<String> keys) {
    this.inKeys = keys;
    return this;
  }

  /**
  * This method adds a collection of fields to the inFields collection.
  * @param fields The fields to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams inFields(String... fields) {
    return inFields(Arrays.asList(fields));
  }

  /**
  * This method adds a collection of fields to the inFields collection.
  * @param fields The collection of fields to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams inFields(Collection<String> fields) {
    if (this.inFields == null) {
      this.inFields = new ArrayList<>(fields);
    } else {
      this.inFields.addAll(fields);
    }
    return this;
  }

  /**
  * This method adds a collection of fields to the returnFields collection.
  * @param fields The fields to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams returnFields(String... fields) {
    if (returnFieldNames != null) {
      Arrays.stream(fields).forEach(f -> returnFieldNames.add(FieldName.of(f)));
    } else {
      if (returnFields == null) {
        returnFields = new ArrayList<>();
      }
      Arrays.stream(fields).forEach(f -> returnFields.add(f));
    }
    return this;
  }

  /**
  * This method adds a field to the returnFieldNames collection.
  * @param field The field to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams returnField(FieldName field) {
    initReturnFieldNames();
    returnFieldNames.add(field);
    return this;
  }

  /**
  * This method adds a collection of fields to the returnFieldNames collection.
  * @param fields The fields to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams returnFields(FieldName... fields) {
    return returnFields(Arrays.asList(fields));
  }

  /**
  * This method adds a collection of fields to the returnFieldNames collection.
  * @param fields The collection of fields to be added.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams returnFields(Collection<FieldName> fields) {
    initReturnFieldNames();
    returnFieldNames.addAll(fields);
    return this;
  }

  private void initReturnFieldNames() {
    if (returnFieldNames == null) {
      returnFieldNames = new ArrayList<>();
    }
    if (returnFields != null) {
      returnFields.forEach(f -> returnFieldNames.add(FieldName.of(f)));
      returnFields = null;
    }
  }

  /**
  * This method sets the summarize flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams summarize() {
    this.summarize = true;
    return this;
  }

  /**
  * This method sets the summarize parameters.
  * @param summarizeParams The summarize parameters to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams summarize(SummarizeParams summarizeParams) {
    this.summarizeParams = summarizeParams;
    return this;
  }

  /**
  * This method sets the highlight flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams highlight() {
    this.highlight = true;
    return this;
  }

  /**
  * This method sets the highlight parameters.
  * @param highlightParams The highlight parameters to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams highlight(HighlightParams highlightParams) {
    this.highlightParams = highlightParams;
    return this;
  }

  /**
   * Set the query custom scorer
   * <p>
   * See http://redisearch.io for documentation on extending RediSearch
   *
   * @param scorer a custom scorer.
   *
   * @return the query object itself
   */
  public FTSearchParams scorer(String scorer) {
    this.scorer = scorer;
    return this;
  }
//
//  public FTSearchParams explainScore() {
//    this.explainScore = true;
//    return this;
//  }

  /**
  * This method sets the slop.
  * @param slop The slop to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams slop(int slop) {
    this.slop = slop;
    return this;
  }

  /**
  * This method sets the timeout.
  * @param timeout The timeout to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams timeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
  * This method sets the inOrder flag to true.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams inOrder() {
    this.inOrder = true;
    return this;
  }

  /**
  * This method sets the language.
  * @param language The language to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams language(String language) {
    this.language = language;
    return this;
  }

  /**
  * This method sets the sortBy and sortOrder.
  * @param sortBy The sortBy to be set.
  * @param order The sortOrder to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams sortBy(String sortBy, SortingOrder order) {
    this.sortBy = sortBy;
    this.sortOrder = order;
    return this;
  }

  /**
  * This method sets the limit.
  * @param offset The offset to be set.
  * @param num The number to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams limit(int offset, int num) {
    this.limit = new int[]{offset, num};
    return this;
  }

  /**
  * This method adds a parameter to the params map.
  * @param name The name of the parameter.
  * @param value The value of the parameter.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams addParam(String name, Object value) {
    if (params == null) {
      params = new HashMap<>();
    }
    params.put(name, value);
    return this;
  }

  /**
  * This method sets the params map to a given map of parameters.
  * @param paramValues The map of parameters to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams params(Map<String, Object> paramValues) {
    if (this.params == null) {
      this.params = new HashMap<>(paramValues);
    } else {
      this.params.putAll(params);
    }
    return this;
  }

  /**
  * This method sets the dialect.
  * @param dialect The dialect to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams dialect(int dialect) {
    this.dialect = dialect;
    return this;
  }

  @Internal
  /**
  * This method sets the dialect only if it is not 0 and the current dialect is null.
  * @param dialect The dialect to be set.
  * @return The current instance of FTSearchParams.
  */
  public FTSearchParams dialectOptional(int dialect) {
    if (dialect != 0 && this.dialect == null) {
      this.dialect = dialect;
    }
    return this;
  }

  /**
  * This method gets the value of the noContent flag.
  * @return The current value of the noContent flag.
  */
  public boolean getNoContent() {
    return noContent;
  }

  /**
  * This method gets the value of the withScores flag.
  * @return The current value of the withScores flag.
  */
  public boolean getWithScores() {
    return withScores;
  }

  public static class NumericFilter implements IParams {

    private final String field;
    private final double min;
    private final boolean exclusiveMin;
    private final double max;
    private final boolean exclusiveMax;

    public NumericFilter(String field, double min, double max) {
      this(field, min, false, max, false);
    }

    public NumericFilter(String field, double min, boolean exclusiveMin, double max, boolean exclusiveMax) {
      this.field = field;
      this.min = min;
      this.max = max;
      this.exclusiveMax = exclusiveMax;
      this.exclusiveMin = exclusiveMin;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(FILTER).add(field)
          .add(formatNum(min, exclusiveMin))
          .add(formatNum(max, exclusiveMax));
    }

    private Object formatNum(double num, boolean exclude) {
      return exclude ? ("(" + num) : Protocol.toByteArray(num);
    }
  }

  public static class GeoFilter implements IParams {

    private final String field;
    private final double lon;
    private final double lat;
    private final double radius;
    private final GeoUnit unit;

    public GeoFilter(String field, double lon, double lat, double radius, GeoUnit unit) {
      this.field = field;
      this.lon = lon;
      this.lat = lat;
      this.radius = radius;
      this.unit = unit;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(GEOFILTER).add(field)
          .add(lon).add(lat)
          .add(radius).add(unit);
    }
  }

  public static class SummarizeParams implements IParams {

    private Collection<String> fields;
    private Integer fragsNum;
    private Integer fragSize;
    private String separator;

    public SummarizeParams() {
    }

    public SummarizeParams fields(String... fields) {
      return fields(Arrays.asList(fields));
    }

    public SummarizeParams fields(Collection<String> fields) {
      this.fields = fields;
      return this;
    }

    public SummarizeParams fragsNum(int num) {
      this.fragsNum = num;
      return this;
    }

    public SummarizeParams fragSize(int size) {
      this.fragSize = size;
      return this;
    }

    public SummarizeParams separator(String separator) {
      this.separator = separator;
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(SUMMARIZE);

      if (fields != null) {
        args.add(FIELDS).add(fields.size()).addObjects(fields);
      }
      if (fragsNum != null) {
        args.add(FRAGS).add(fragsNum);
      }
      if (fragSize != null) {
        args.add(LEN).add(fragSize);
      }
      if (separator != null) {
        args.add(SEPARATOR).add(separator);
      }
    }
  }

  public static SummarizeParams summarizeParams() {
    return new SummarizeParams();
  }

  public static class HighlightParams implements IParams {

    private Collection<String> fields;
    private String[] tags;

    public HighlightParams() {
    }

    public HighlightParams fields(String fields) {
      return fields(Arrays.asList(fields));
    }

    public HighlightParams fields(Collection<String> fields) {
      this.fields = fields;
      return this;
    }

    public HighlightParams tags(String open, String close) {
      this.tags = new String[]{open, close};
      return this;
    }

    @Override
    public void addParams(CommandArguments args) {
      args.add(HIGHLIGHT);

      if (fields != null) {
        args.add(FIELDS).add(fields.size()).addObjects(fields);
      }
      if (tags != null) {
        args.add(TAGS).add(tags[0]).add(tags[1]);
      }
    }
  }

  public static HighlightParams highlightParams() {
    return new HighlightParams();
  }
}
