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
  * Default constructor for FTSearchParams.
  */
  public FTSearchParams() {
  }

  /**
  * Static factory method to create FTSearchParams.
  * @return FTSearchParams instance.
  */
  public static FTSearchParams searchParams() {
    return new FTSearchParams();
  }

  @Override
  /**
  * Add parameters to the CommandArguments.
  * @param args The CommandArguments to add parameters to.
  */
  /**
  * Add parameters to the CommandArguments for NumericFilter.
  * @param args The CommandArguments to add parameters to.
  */
  /**
  * Add parameters to the CommandArguments for GeoFilter.
  * @param args The CommandArguments to add parameters to.
  */
  /**
  * Add parameters to the CommandArguments for SummarizeParams.
  * @param args The CommandArguments to add parameters to.
  */
  /**
  * Add parameters to the CommandArguments for HighlightParams.
  * @param args The CommandArguments to add parameters to.
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
  * Set the 'no content' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams noContent() {
    this.noContent = true;
    return this;
  }

  /**
  * Set the 'verbatim' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams verbatim() {
    this.verbatim = true;
    return this;
  }

  /**
  * Set the 'no stopwords' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams noStopwords() {
    this.noStopwords = true;
    return this;
  }

  /**
  * Set the 'with scores' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams withScores() {
    this.withScores = true;
    return this;
  }

  /**
  * Add a numeric filter with min and max values.
  * @param field The field name.
  * @param min The minimum value.
  * @param max The maximum value.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams filter(String field, double min, double max) {
    return filter(new NumericFilter(field, min, max));
  }

  /**
  * Add a numeric filter with min, max, and exclusivity flags.
  * @param field The field name.
  * @param min The minimum value.
  * @param exclusiveMin Whether the minimum value is exclusive.
  * @param max The maximum value.
  * @param exclusiveMax Whether the maximum value is exclusive.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams filter(String field, double min, boolean exclusiveMin, double max, boolean exclusiveMax) {
    return filter(new NumericFilter(field, min, exclusiveMin, max, exclusiveMax));
  }

  /**
  * Add a numeric filter.
  * @param numericFilter The numeric filter.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams filter(NumericFilter numericFilter) {
    filters.add(numericFilter);
    return this;
  }

  /**
  * Add a geo filter with field, coordinates, radius, and unit.
  * @param field The field name.
  * @param lon The longitude.
  * @param lat The latitude.
  * @param radius The radius.
  * @param unit The unit of the radius.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams geoFilter(String field, double lon, double lat, double radius, GeoUnit unit) {
    return geoFilter(new GeoFilter(field, lon, lat, radius, unit));
  }

  /**
  * Add a geo filter.
  * @param geoFilter The geo filter.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams geoFilter(GeoFilter geoFilter) {
    filters.add(geoFilter);
    return this;
  }

  /**
  * Set the 'in keys' filter.
  * @param keys The keys to filter.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams inKeys(String... keys) {
    return inKeys(Arrays.asList(keys));
  }

  /**
  * Set the 'in keys' filter.
  * @param keys The keys to filter.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams inKeys(Collection<String> keys) {
    this.inKeys = keys;
    return this;
  }

  /**
  * Set the 'in fields' filter.
  * @param fields The fields to filter.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams inFields(String... fields) {
    return inFields(Arrays.asList(fields));
  }

  /**
  * Set the 'in fields' filter.
  * @param fields The fields to filter.
  * @return Modified FTSearchParams instance.
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
  * Set the 'return fields' filter.
  * @param fields The fields to return.
  * @return Modified FTSearchParams instance.
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
  * Set a single 'return field'.
  * @param field The field to return.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams returnField(FieldName field) {
    initReturnFieldNames();
    returnFieldNames.add(field);
    return this;
  }

  /**
  * Set multiple 'return fields'.
  * @param fields The fields to return.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams returnFields(FieldName... fields) {
    return returnFields(Arrays.asList(fields));
  }

  /**
  * Set multiple 'return fields'.
  * @param fields The fields to return.
  * @return Modified FTSearchParams instance.
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
  * Set the 'summarize' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams summarize() {
    this.summarize = true;
    return this;
  }

  /**
  * Set the 'summarize' flag with custom parameters.
  * @param summarizeParams The summarize parameters.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams summarize(SummarizeParams summarizeParams) {
    this.summarizeParams = summarizeParams;
    return this;
  }

  /**
  * Set the 'highlight' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams highlight() {
    this.highlight = true;
    return this;
  }

  /**
  * Set the 'highlight' flag with custom parameters.
  * @param highlightParams The highlight parameters.
  * @return Modified FTSearchParams instance.
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
  /**
  * Set the query custom scorer.
  * @param scorer The custom scorer.
  * @return Modified FTSearchParams instance.
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
  * Set the slop value.
  * @param slop The slop value.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams slop(int slop) {
    this.slop = slop;
    return this;
  }

  /**
  * Set the timeout value.
  * @param timeout The timeout value.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams timeout(long timeout) {
    this.timeout = timeout;
    return this;
  }

  /**
  * Set the 'in order' flag.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams inOrder() {
    this.inOrder = true;
    return this;
  }

  /**
  * Set the language.
  * @param language The language.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams language(String language) {
    this.language = language;
    return this;
  }

  /**
  * Set the sort by field and order.
  * @param sortBy The field to sort by.
  * @param order The sorting order.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams sortBy(String sortBy, SortingOrder order) {
    this.sortBy = sortBy;
    this.sortOrder = order;
    return this;
  }

  /**
  * Set the limit for results.
  * @param offset The offset.
  * @param num The number of results.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams limit(int offset, int num) {
    this.limit = new int[]{offset, num};
    return this;
  }

  /**
  * Add a custom parameter.
  * @param name The parameter name.
  * @param value The parameter value.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams addParam(String name, Object value) {
    if (params == null) {
      params = new HashMap<>();
    }
    params.put(name, value);
    return this;
  }

  /**
  * Set multiple custom parameters.
  * @param paramValues The parameter values.
  * @return Modified FTSearchParams instance.
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
  * Set the dialect.
  * @param dialect The dialect.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams dialect(int dialect) {
    this.dialect = dialect;
    return this;
  }

  @Internal
  /**
  * Set the optional dialect.
  * @param dialect The optional dialect.
  * @return Modified FTSearchParams instance.
  */
  public FTSearchParams dialectOptional(int dialect) {
    if (dialect != 0 && this.dialect == null) {
      this.dialect = dialect;
    }
    return this;
  }

  /**
  * Get the 'no content' flag.
  * @return True if 'no content' flag is set, false otherwise.
  */
  public boolean getNoContent() {
    return noContent;
  }

  /**
  * Get the 'with scores' flag.
  * @return True if 'with scores' flag is set, false otherwise.
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

    /**
    * Constructor for NumericFilter with min and max values.
    * @param field The field name.
    * @param min The minimum value.
    * @param max The maximum value.
    */
    public NumericFilter(String field, double min, double max) {
      this(field, min, false, max, false);
    }

    /**
    * Constructor for NumericFilter with min, max, and exclusivity flags.
    * @param field The field name.
    * @param min The minimum value.
    * @param exclusiveMin Whether the minimum value is exclusive.
    * @param max The maximum value.
    * @param exclusiveMax Whether the maximum value is exclusive.
    */
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

    /**
    * Constructor for GeoFilter with field, coordinates, radius, and unit.
    * @param field The field name.
    * @param lon The longitude.
    * @param lat The latitude.
    * @param radius The radius.
    * @param unit The unit of the radius.
    */
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

    /**
    * Default constructor for SummarizeParams.
    */
    public SummarizeParams() {
    }

    /**
    * Set the fields for summarization.
    * @param fields The fields to summarize.
    * @return Modified SummarizeParams instance.
    */
    public SummarizeParams fields(String... fields) {
      return fields(Arrays.asList(fields));
    }

    public SummarizeParams fields(Collection<String> fields) {
      this.fields = fields;
      return this;
    }

    /**
    * Set the number of fragments for summarization.
    * @param num The number of fragments.
    * @return Modified SummarizeParams instance.
    */
    public SummarizeParams fragsNum(int num) {
      this.fragsNum = num;
      return this;
    }

    /**
    * Set the fragment size for summarization.
    * @param size The fragment size.
    * @return Modified SummarizeParams instance.
    */
    public SummarizeParams fragSize(int size) {
      this.fragSize = size;
      return this;
    }

    /**
    * Set the separator for summarization.
    * @param separator The separator.
    * @return Modified SummarizeParams instance.
    */
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

    /**
    * Default constructor for HighlightParams.
    */
    public HighlightParams() {
    }

    /**
    * Set the fields for highlighting.
    * @param fields The fields to highlight.
    * @return Modified HighlightParams instance.
    */
    public HighlightParams fields(String fields) {
      return fields(Arrays.asList(fields));
    }

    public HighlightParams fields(Collection<String> fields) {
      this.fields = fields;
      return this;
    }

    /**
    * Set the open and close tags for highlighting.
    * @param open The opening tag.
    * @param close The closing tag.
    * @return Modified HighlightParams instance.
    */
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
