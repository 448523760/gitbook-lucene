# lucene gerneral

## Lucene ThreadLocal

**Java的ThreadLocal类有一个严重的瑕疵那就是即便在ThreadLocal 实例本身不在被引用的情况下，其中存储的东西也需要相当长的时间才能被解除引用。**原因是所有的ThreadLocal 实例共享一个map，而该map只会在指定的时间段内清除过期的条目。CloseableThreadLocal通过接收WeakReference类型的值并且保持对每个存储值一个硬应用，当调用close()方法的时候会清楚所有的引用，这样GC就可以回收所占用的内存空间。

## Lock文件

写锁（write lock）文件名为“write.lock”，它缺省存储在索引目录中。如果锁目录（lock directory）与索引目录不一致，写锁将被命名为“XXXX-write.lock”，其中“XXXX”是一个唯一的前缀（unique prefix），来源于（derived from）索引目录的全路径（full path）。当这个写锁出现时，一个writer当前正在修改索引（添加或者清除文档）。这个写锁确保在一个时刻只有一个writer修改索引。

## Compound File 组合文件

CompoundFileDirectory:

属于同一个段的所有文件具有相同的文件名和不同的后缀名，不同的后缀名是因为
Codec采用了不同的文件格式(file formats). 当使用组合文件的时候，所有的段文件
会合并到一个.cfs后缀的文件内（除了LiveDocsFormat使用.cfe文件）；

文件的结构组成：
* Header：
* FileCount：记录有多少文件被整合到了组合文件当中。
* DataOffset,DataLength：
* FileName
* FileData

## 删除文档文件(.del)

如果segment中有被删除的document，则这些信息会被存储在.del文件当中。

.del文件的结构：

* Format：在此文件中，Bits 和DGaps 只能保存其中一，-1 表示保存DGaps，非负值表示保存Bits。
* ByteCount：此段中有多少文档，就有多少个bit 被保存，但是以byte 形式计数，即Bits 的大小应该是byte 的倍数。
* BitCount：Bits 中有多少位被至1，表示此文档已经被删除。
* Bits：一个数组的byte，大小为ByteCount，应用时被认为是byte*8 个bit。
* DGaps：如果删除的文档数量很小，则Bits 大部分位为0，很浪费空间。DGaps 采用以下的方式来保存稀疏数组：比如第十，十二，三十二个文档被删除，于是第十，十二，三十二位设为1，DGaps 也是以byte 为单位的，仅保存不为0 的byte，如第1 个byte，第4 个byte，第1 个byte 十进制为20，第4 个byte 十进制为1。于是保存成DGaps，第1 个byte，位置1 用不定长正整数保存，值为20 用二进制保存，第2 个byte，位置4 用不定长正整数保存，用差值为3，值为1 用二进制保存，二进制数据不用差值表示。