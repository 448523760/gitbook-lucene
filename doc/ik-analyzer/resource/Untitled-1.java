/**
* 加载主词典及扩展词典
*/
private void loadMainDict(){
//建立一个主词典实例
_MainDict = new DictSegment((char)0);
//读取主词典文件
InputStream is = this.getClass().getClassLoader().getResourceAsStream(cfg.getMainDictionary());
if(is == null){
throw new RuntimeException("Main Dictionary not found!!!");
}
try {
BufferedReader br = new BufferedReader(new InputStreamReader(is , "UTF-8"), 512);
String theWord = null;
do {
theWord = br.readLine();
if (theWord != null && !"".equals(theWord.trim())) {
_MainDict.fillSegment(theWord.trim().toLowerCase().toCharArray());//加载主词典
}
} while (theWord != null);
} catch (IOException ioe) {
System.err.println("Main Dictionary loading exception.");
ioe.printStackTrace();
}finally{
try {
if(is != null){
is.close();
is = null;
}
} catch (IOException e) {
e.printStackTrace();
}
}
//加载扩展词典
this.loadExtDict();