# Chapter 2

## Analyzing Your Text

A term is a fundamental unit of data in a Lucene index. It associates with a Document and itself has two attributes – field and value. An analyzer is responsible for generating these terms. An analyzer is a container of tokenization and filtering processes. Tokenization is a process that breaks up text at word boundaries defined by a specific tokenizer component. 

![analyzer-process](./resource/analyzer-process.jpg)
In this illustration, a tokenizer uses a reader object to consume text. It produces a sequential set of tokens that is called TokenStream. TokenFilter accepts the TokenStream, applies the filtering process, and emits filtered data in TokenStream in return. TokenFilters can be chained together to attain the desired results. A character filter can also be used to preprocess data before tokenization. One example use case for character filters is stripping out HTML tags.

* Stopword filtering
* Text normalization
* Stemming: Snowball, PorterStem, and KStem
* Synonym Expansion(同义词扩展): As the name suggests, this technique expands a word into additional similar-meaning words for matching,

## Obtaining a common analyzer

Lucene provides a set of default analyzers in the **lucene-analyzers-common** package.

* WhitespaceAnalyzer: Splits text at whitespaces, just as the name indicates.
* SimpleAnalyzer: Splits text at non-letter characters and lowercases resulting tokens.
* StopAnalyzer: Splits text at non-letter characters, lowercases resulting tokens, and removes stopwords. This analyzer comes with a default set of stopwords but you can always have the provision to provide your own set of stopwords.
* StandardAnalyzer: Splits text using a grammar-based tokenization, normalizes and lowercases tokens, removes stopwords, and discards punctuations.

> TIP Make sure the lucene-analyzers-common.jar library is also added to the classpath or the corresponding dependency in your pom.xml.

The following would be the output of a WhitespaceAnalyzer:
`[Lucene] [is] [mainly] [used] [for] [information] [retrieval] [and] [you] [can] [read] [more] [about] [it] [at] [lucene.apache.org.]`

SimpleAnalyzer:
`[lucene] [is] [mainly] [used] [for] [information] [retrieval] [and] [you] [can] [read] [more] [about] [it] [at] [lucene] [apache] [org]`

StopAnalyzer:
`[lucene] [mainly] [used] [information] [retrieval] [you] [can] [read] [more] [about] [lucene] [apache] [org]`

StandardAnalyzer:
`[lucene] [mainly] [used] [information] [retrieval] [you] [can] [read] [more] [about] [lucene.apache.org]`

## Obtaining a TokenStream

TokenStream is an intermediate data format between components within the analysis process. TokenStream acts as both an input and output format in all filters. For tokenizer, it consumes text from a reader and outputs result as TokenStream. Let's explore TokenStream in detail in this section.

> tokenStream 

```java
Reader reader = new StringReader("Text to be passed");
Analyzer analyzer = new SimpleAnalyzer();
TokenStream tokenStream = analyzer.tokenStream("myField", reader);
```

TokenStream extends from `AttributeSource` and it provides an interface to return token attributes and value.

## Obtaining TokenAttribute values

From a high level, TokenStream is an enumeration of tokens. To access the values, we will provide TokenStream with one or more attribute objects. Note that there is only one instance that exists per attribute. This is for performance reasons so we are not creating objects in each iteration; instead, the same attribute instances are updated when we increment the token.

* CharTermAttribute: This exposes a token's actual textual value, equivalent to a term's value.

* PositionIncrementAttribute: This returns the position of the current token relative to the previous token. This attribute is useful in phrase-matching as the keyword order and their positions are important. If there are no gaps between the current token and the previous token (for example, no stop words in between), it will be set to its default value, 1.

* OffsetAttribute: This gives you information about the start and end positions of the corresponding term in the source text.

* TypeAttribute: This is available if it is used in the implementation. This is usually used to identify the data type.

* FlagsAttribute: This is somewhat similar to TypeAttribute, but it serves a different purpose. Suppose you need to add specific information about a token and that information should be available down the analyzer chain, you can pass it as flags. TokenFilters can perform any specific action based on the flags of the token.

* PayloadAttribute: This stores the payload at each index position and is generally useful in scoring when used with Payload-based queries. Because it's stored at each position, it is best to have a minimum number of bytes per term in the index to minimize overloading the index with a massive amount of data.

Now we will see Attribute retrieval in action. In this sample, we will use StandardAnalyzer to process the input text and OffsetAttribute and CharTermAttribute to return each token's value and its offsets. Here is the sample code:

```java
StringReader reader = new StringReader("Lucene is mainly used for information retrieval and you can read more about it at lucene.apache.org.");
StandardAnalyzer wa = new StandardAnalyzer();
TokenStream ts = null;

try {
    ts = wa.tokenStream("field", reader);

    OffsetAttribute offsetAtt = ts.addAttribute(OffsetAttribute.class);
    CharTermAttribute termAtt = ts.addAttribute(CharTermAttribute.class);

    ts.reset();

    while (ts.incrementToken()) {
        String token = termAtt.toString();
        System.out.println("[" + token + "]");
        System.out.println("Token starting offset: " + offsetAtt.startOffset());
        System.out.println(" Token ending offset: " + offsetAtt.endOffset());
        System.out.println("");
    }

    ts.end();
} catch (IOException e) {
    e.printStackTrace();
} finally {
    ts.close();
    wa.close();
}
```

***Keep in mind that Attribute objects are reused in each iteration as we increment tokens for performance and efficient memory management.***

How it works…

1. To start processing text, we turn our input stream into StringReader to pass into the Analyzer's tokenStream method.
2. Then we instantiate two attribute objects, OffsetAttribute and CharTermAttribute.
3. The attribute objects are then registered in TokenStream by calling its addAttribute method.
4. Note that we call ts.reset() to reset TokenStream to the beginning. This call is necessary prior to every iteration routine to ensure we always iterate from the beginning.
5. We iterate TokenStream in a while loop by calling ts.incrementToken(). The loop exits when incrementToken() returns false.
6. We call termAtt.toString() to return the current token's value and call the startOffset() and endOffset() methods of offsetAtt to get the offset. Note that the     variables termAtt and offsetAtt are reused in every iteration.
7. Now we call ts.end() to end the TokenStream. This call signals the current TokenStream handler to execute any end-of-stream operations.
8. And lastly, we call the close() method to close out the TokenStream and Analyzer to release any resources used during the analysis process.

## Using PositionIncrementAttribute

The PositionIncrementAttribute class shows the position of the current token relative to the previous token. The default value is 1. Any value greater than 1 implies that the previous token and the current token are not consecutive – there is a gap between the two tokens where some tokens (for example, stopwords) are omitted.

This attribute is useful in phrase matching, where the position and order of words matters. For example, say you want to execute an exact phrase match. As we step through TokenStream, the PositionIncrementAttribute class on each matching token should be 1 so we know the phrase we are matching is matched word for word exactly in the same order as the search phrase.



## todo add missing page

add missing page between Using `PositionIncrementAttribute` and `Defining custom TokenFilters`

![book](https://learning.oreilly.com/library/view/lucene-4-cookbook/9781782162285/ch02s07.html)


## Defining custom TokenFilters

Sometimes, search behaviors may be so specific that we need to create a custom TokenFilter to achieve those behaviors. To create a custom filter, we will extend from the TokenFilter class and override the incrementToken() method.

> We will create a simple word-expanding TokenFilter that expands courtesy titles from the short form to the full word. For example, Dr expands to doctor.
```java
public class CourtesyTitleFilter extends TokenFilter {
    Map<String,String> courtesyTitleMap = new HashMap<String,String>();
    private CharTermAttribute termAttr;
    public CourtesyTitleFilter(TokenStream input) {
        super(input);
        termAttr = addAttribute(CharTermAttribute.class);
        courtesyTitleMap.put("Dr", "doctor");
        courtesyTitleMap.put("Mr", "mister");
        courtesyTitleMap.put("Mrs", "miss");
    }
    public boolean incrementToken() throws IOException {
        if (!input.incrementToken())
            return false;
        String small = termAttr.toString();
        if(courtesyTitleMap.containsKey(small)) {
            termAttr.setEmpty().append(courtesyTitleMap.get(small));
        }
        return true;
    }
}
```

## Defining custom analyzers

The anatomy of an analyzer includes one tokenizer and one or more TokenFilters. We will build an Analyzer by extending from the Analyzer abstract class and implement the createComponents method.

```java
public class CourtesyTitleAnalyzer extends Analyzer {

    @Override
    protected TokenStreamComponents createComponents(String fieldName, Reader reader) {
    Tokenizer letterTokenizer = new LetterTokenizer(reader);
    TokenStream filter = new CourtesyTitleFilter(letterTokenizer);
    return new TokenStreamComponents(letterTokenizer, filter);
  }
}
```

Note that the only method we need to override is createComponents. We don't need to override the constructor to build our Analyzer because components are not added during construction; they are added when the createComponents method is called. Therefore, we override the createComponents method to customize an Analyzer. Also note that we cannot override the tokenStream method because it's declared as final.

## Defining custom tokenizers

**Lucene provides a character-based tokenizer called CharTokenizer that should be suitable for most types of tokenizations. You can override its isTokenChar method to determine what characters should be considered as part of a token and what characters should be considered as delimiters. It's worthwhile to note that both LetterTokenizer and WhitespaceTokenizer extend from CharTokenizer.**

> Sample WhitespaceTokenizer 

```java
public class MyTokenizer extends CharTokenizer {

    public MyTokenizer(Reader input) {
        super(input);
    }

    public MyTokenizer(AttributeFactory factory, Reader input) {
        super(factory, input);
    }

    @Override
    protected boolean isTokenChar(int c) {
        return !Character.isSpaceChar(c);
    }
}
```

In this example, we extend from an abstract class called CharTokenizer. As described earlier, this is a character-based tokenizer. To use CharTokenizer, you need to override the isTokenChar method. In this method, you get to examine the input stream (via Reader) character by character and determine whether to treat the character as a token character or a delimiting character. It handles the complexity of token extraction from a Reader for you so you can focus on the business logic of how text should be tokenized. We want to build a tokenizer that splits text by space only, so we leverage the isSpaceChar method from the character class to determine if the character is a space. If it's a space, it returns false, which means it's a token character. Otherwise, the character will be treated as a delimiting character and a new token will form afterwards.

## Defining custom attributes

The built-in Attribute classes should suffice for most of the implementation, but if you encounter a situation where you need a custom attribute, you may do so by creating your own Attribute class. In this section, we will create an Attribute interface and then an implementation class extending AttributeImpl and a TokenFilter that will set the attribute value. Our example will be a simple exercise where we determine the gender for a token based on specific words. This is purely for illustration purpose only, so this is by no means a useful implementation for any application.

```java
public interface GenderAttribute extends Attribute {

    public static enum Gender {Male, Female, Undefined};

    public void setGender(Gender gender);

    public Gender getGender();
}

public class GenderAttributeImpl extends AttributeImpl implements GenderAttribute {

    private Gender gender = Gender.Undefined;

    public void setGender(Gender gender) {
      this.gender = gender;
    }

    public Gender getGender() {
      return gender;
    };
    @Override
    public void clear() {
        gender = Gender.Undefined;
    }

    @Override
    public void copyTo(AttributeImpl target) {
        ((GenderAttribute) target).setGender(gender);
    }
}

public class GenderFilter extends TokenFilter {
    GenderAttribute genderAtt = addAttribute(GenderAttribute.class);
    CharTermAttribute charTermAtt = addAttribute(CharTermAttribute.class);

    protected GenderFilter(TokenStream input) {
        super(input);
    }

    public boolean incrementToken() throws IOException {
        if (!input.incrementToken()) { return false; }
        genderAtt.setGender(determineGender(charTermAtt.toString()));
        return true;
    }

    protected GenderAttribute.Gender determineGender(String term) {
        if (term.equalsIgnoreCase("mr") || term.equalsIgnoreCase("mister")) {
            return GenderAttribute.Gender.Male;
        } else if (term.equalsIgnoreCase("mrs") || term.equalsIgnoreCase("misters")) {
            return GenderAttribute.Gender.Female;
        }
        return GenderAttribute.Gender.Undefined;
    }
}
```

Part of the requirement of creating a custom Attribute class is the creation of an interface that's based on attribute and an implementation that's based on AttributeImpl. The names of the classes are important because Lucene looks for the Impl suffix to locate the implementation class of an Attribute interface.

Let's dive into the details of the implementation now. In GenderAttribute, we defined it as an interface-extending attribute. This is what will be passed into the addAttribute method to register this attribute to a TokenStream. We defined an enum that holds three values – Male, Female, and Undefined. We also defined a setter and a getter for the gender variable.

In the implementation class GenderAttributeImpl, we have a private variable, gender, that will store the gender value for a token and we have implementations for the setter and getter defined in the interface. We also overrode two methods – clear and copyTo. Overriding the clear method allows us to customize how we clear the attribute value and overriding copyTo allows us to customize how copy attribute is done.

And lastly, we have GenderFilter, which extends TokenFilter. We instantiate two attributes – GenderAttribute (what we defined) and CharTermAttribute. We will use CharTermAttribute to return the textual value of a token so we can determine the gender from it. We implemented incrementToken so we can insert the gender into GenderAttribute when incrementToken is called. The gender is determined in the determineGender method. The implementation of this method just simply checks if the current term matches a certain courtesy title to determine the gender. Then it returns the gender value. This implementation is intentionally simplified, as the robustness of this method is not the priority of this section.
