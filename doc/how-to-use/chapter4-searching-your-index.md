# Chapter 4. Searching Your Indexes

## Obtaining IndexReaders

Lucene provides the IndexReader class to access a point-in-time view of an index. It means that you can concurrently write to an index, while an existing IndexReader is reading without exposing any uncommitted data to the active IndexReader. This is an important concept to keep in mind because this architecture allows the possibility of **providing a seamless transition between index versions by opening a new IndexReader while the old IndexReader is still servicing a search.** The DirectoryReader is a subclass of IndexReader, which is the class that provides the facility to actually open a directory containing an index, and returns an IndexReader. **DirectoryReader also has a more optimized index opening method called openIfChanged that will reuse the existing DirectoryReader for faster reopening of an index.**

IndexReader, by itself, is an abstract class. It has two implementations: AtomicReader and CompositeReader. This AtomicReader is a single reader that's atomic for a single segment of an index that supports retrieval of stored fields, doc values, terms, and postings. This CompositeReader contains a list of AtomicReaders on multiple segments. It supports retrieval of stored fields only because the other properties (for example, norm values and doc values) need to be merged from the underlying AtomicReaders. It's up to the programmer to code facility classes by iterating through CompositeReader.leaves() to merge the results, or use a built-in class called SlowCompositeReaderWrapper to do the merge, although it has a significant performance hit from scanning the underlying AtomicReaders.

In our example, we will be using DirectoryReader to open an index. Note that DirectoryReader is an implementation of CompositeReader, so by calling its open method, it will return a CompositeReader.

```java
// open a directoryDirectory directory = FSDirectory.open(
    new File("/data/index"));// set up a DirectoryReaderDirectoryReader directoryReader = 
    DirectoryReader.open(directory);// pull a list of underlying AtomicReadersList<AtomicReaderContext> atomicReaderContexts = 
    directoryReader.leaves();// retrieve the first AtomicReader from the listAtomicReader atomicReader = 
        atomicReaderContexts.get(0).reader();// open another DirectoryReader by calling openIfChangedDirectoryReader newDirectoryReader = 
    DirectoryReader.openIfChanged(directoryReader);// assign newDirectoryReaderif (newDirectoryReader != null) {
    IndexSearcher indexSearcher =
        new IndexSearcher(newDirectoryReader);
    // close the old DirectoryReader
    directoryReader.close();
}
```

Note again that we obtained a DirectoryReader (essentially, an IndexReader) by calling DirectoryReader.open(Directory). To gain access to the AtomicReader underneath, we need to call the leaves() method to retrieve a list of AtomicReaderContext, and from AtomicReaderContext, we can access AtomicReader. After we gain access to an AtomicReader, we have a demonstration of calling openIfChanged(IndexReader). This call will return a new DirectoryReader if there is a change to the index. It returns null if the index has not changed.

## Un-inverting single-valued fields in memory with FieldCache

We have learned very early on that Lucene stores data in an inverted index in which terms are sorted and DocId is associated with each term. A search is essentially finding DocId's intersection in matched terms. The index itself allows for a very fast term lookup, but it's not ideal for lookup by DocId. To solve the problem of finding a field value by DocId, Lucene introduced FieldCache. FieldCache is an in-memory data structure that stored in an array format in which the value position corresponds to DocId (since DocId is basically an ordinal value of all documents). Because each array position can only store one value, FieldCache should be used on single-valued fields only. In essence, Lucene uninverts data from the index and stores them in FieldCache.

> Note that when FieldCache is initialized, it stays static and it does not synchronize with IndexReader. If you need to reopen IndexReader, FieldCache will need to be reinitialized in order to be in sync with the latest changes.

```java
StandardAnalyzer analyzer = new StandardAnalyzer();
Directory directory = new RAMDirectory();
IndexWriterConfig config =
    new IndexWriterConfig(Version.LATEST, analyzer);
IndexWriter indexWriter = new IndexWriter(directory, config);

Document doc = new Document();
StringField stringField =
    new StringField("name", "", Field.Store.YES);

String[] contents = {"alpha", "bravo", "charlie",
    "delta", "echo", "foxtrot"};
for (String content : contents) {
    stringField.setStringValue(content);
    doc.removeField("name");
    doc.add(stringField);
    indexWriter.addDocument(doc);
}

indexWriter.commit();

IndexReader indexReader = DirectoryReader.open(directory);

BinaryDocValues cache = FieldCache.DEFAULT.getTerms(
    SlowCompositeReaderWrapper.wrap(indexReader), "name", true);

for (int i = 0; i < indexReader.maxDoc(); i++) {
    BytesRef bytesRef = cache.get(i);
    System.out.println(i + ": " + bytesRef.utf8ToString());
}
```

### How it works

Here, we set up our index in a RAMDirectory using a StandardAnalyzer. The content we added to the index is an array of strings. To initialize FieldCache, we made a call to FieldCache.DEFAULT.getTerms(AtomicReader, String, String). FieldCache.DEFAULT is a built-in singleton with the facilities to build and hold on to all the FieldCache we are initializing. In this sample code, we call the getTerms method to build a term-based FieldCache. Note that we wrap our IndexReader with a SlowCompositeReaderWrapper to simulate an AtomicReader, which is required to build FieldCache. The return object is a BinaryDocValues object, where we can retrieve a field value by DocId by calling the get method. We demonstrated this functionality by iterating the cache in a loop. This code should produce the following output:

```txt
0: alpha
1: bravo
2: charlie
3: delta
4: echo
5: foxtrot
```

## TermVectors

TermVectors is a feature in Lucene that lets you retrieve per document term-based statistical data from the index. These additional data points can be useful for features such as highlighting or any term-based reports analysis. As you may expect, this feature is not enabled by default, as it can be expensive to compute these data points and it would increase the index size significantly.

This TermVectors provides the following additional data points for each document:

* Term frequency
* Term position(s)
* Term offsets

Term frequency is the number of times the term appears in a document. Positions is the term in a document where each position is incremented by term. offsets has a starting and ending positions by characters where the term can be located in a document.

Let's look at an example of what you can expect to see in TermVectors. Here is a piece of text to be added to a document:

```txt
humpty dumpty sat on a wall
```

Here is what you will retrieve from TermVectors:

| Term  | Frequency  | Position  | Offset  |
|---|---|---|---|---|
| dumpty  | 1  | 1  | [7,13] |
| humpty  | 1  | 0  | [0,6]  |
| sat     | 1  | 2  | [14,17]  |
| wall    | 1  | 5  | [23,27]  |

> Let's look at a code sample on how to set up and retrieve TermVectors:

```java
StandardAnalyzer analyzer = new StandardAnalyzer();
Directory directory = new RAMDirectory();
IndexWriterConfig config = new IndexWriterConfig(Version.LATEST, analyzer);
IndexWriter indexWriter = new IndexWriter(directory, config);

FieldType textFieldType = new FieldType();
textFieldType.setIndexed(true);
textFieldType.setTokenized(true);
textFieldType.setStored(true);
textFieldType.setStoreTermVectors(true);
textFieldType.setStoreTermVectorPositions(true);
textFieldType.setStoreTermVectorOffsets(true);

Document doc = new Document();
Field textField = new Field("content", "", textFieldType);

String[] contents = {"Humpty Dumpty sat on a wall,","Humpty Dumpty had a great fall.","All the king's horses and all the king's men","Couldn't put Humpty together again."};
for (String content : contents) {
    textField.setStringValue(content);
    doc.removeField("content");
    doc.add(textField);
    indexWriter.addDocument(doc);
}

indexWriter.commit();
IndexReader indexReader = DirectoryReader.open(directory);
DocsAndPositionsEnum docsAndPositionsEnum = null;
Terms termsVector = null;
TermsEnum termsEnum = null;
BytesRef term = null;
String val = null;

for (int i = 0; i < indexReader.maxDoc(); i++) {
    termsVector = indexReader.getTermVector(i, "content");
    termsEnum = termsVector.iterator(termsEnum);
    while ( (term = termsEnum.next()) != null ) {
        val = term.utf8ToString();
        System.out.println("DocId: " + i);
        System.out.println("  term: " + val);
        System.out.println("  length: " + term.length);
        docsAndPositionsEnum = 
            termsEnum.docsAndPositions(null, 
                docsAndPositionsEnum);
        if (docsAndPositionsEnum.nextDoc() >= 0) {
            int freq = docsAndPositionsEnum.freq();
            System.out.println("  freq: " + docsAndPositionsEnum.freq());
            for (int j = 0; j < freq; j++) {
                System.out.println("    [");
                System.out.println("      position: " + docsAndPositionsEnum.nextPosition());
                System.out.println("      offset start: " + docsAndPositionsEnum.startOffset());
                System.out.println("      offset end: " + docsAndPositionsEnum.endOffset());
                System.out.println("    ]");
            }
        }
    }
}
```

To enable TermVectors, we need to set up our own FieldType so we can turn on the TermVectors features. In our example, we enabled TermVectors on a text field that is analyzed by StandardAnalyzer. To retrieve the TermVectors, we called getTermVectors on IndexReader passing in a DocId and field name. This will return TermVectors for a particular document. Then, we iterate it using TermsEnum and retrieve the position and offset attributes with DocsAndPositionsEnum. In TermsEnum, we can retrieve terms and their length. In DocsAndPositionsEnum, we can retrieve term frequency, position(s), and offset. Note that the same term may appear multiple times in a document; hence, the second loop will iterate DocsAndPositionsEnum. Also, note that we need to call nextPosition() to iterate through DocsAndPositionsEnum.

```txt
humpty dumpty sat on a wall
```

```output
DocId: 0
  term: dumpty
  length: 6
  freq: 1
    [
      position: 1
      offset start: 7
      offset end: 13
    ]
DocId: 0
  term: humpty
  length: 6
  freq: 1
    [
      position: 0
      offset start: 0
      offset end: 6
    ]
```

> Note that the terms are sorted alphabetically. TermVector stats can be found within the brackets.

## IndexSearcher

Before we start a search, we need to obtain an IndexSearcher to help us facilitate the querying of an index. IndexSearcher provides a number of search methods for querying data and returning TopDocs as results. TopDocs represents hits from a search and contains an array of ScoreDoc where it contains DocId and the score of each matching document. Note that TopDocs contains DocId and does not actually contain any document content. Document content retrieval will have to be relied on IndexReader or FieldCache. An IndexSearcher requires an IndexReader as an input to its constructor to initialize.

```java
Directory directory = 
    FSDirectory.open(new File("/data/index"));
DirectoryReader directoryReader = DirectoryReader.open(directory);
IndexSearcher indexSearcher = new IndexSearcher(directoryReader);
```

First, we set up a Directory so that we can pass it onto a DirectoryReader, which is basically an IndexReader. Then, we pass the DirectoryReader to IndexSearcher for initialization. **Note that IndexReader is fixated on a point-in-time snapshot of the index. If the index is updated, we need to make sure we reopen the IndexReader and IndexSearcher to expose the latest index changes to search.**
