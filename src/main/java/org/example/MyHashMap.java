package org.example;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

public class MyHashMap<K, V> {

    Map<String, String> map = new HashMap<>();


    // переменные для обычной и мах ёмкости, коэф загрузки при расширении внутреннего массива
    private static final int DEFAULT_INIT_CAP = 1 << 4;
    private static final int MAXIMUM_CAPACITY = 1 << 30;
    private static final float DEFAULT_LOAD_FACTOR = 0.75f;

    // массив бакетов, длина всегда степень двойки
    private transient Node<K,V>[] table;

    // размер, количество пар K,V в map
    private int size;

    // порог, после превышения которого выполняется resize()
    // threshold = capacity * loadFactor
    private int threshold;

    // коэф загрузки
    private final float loadFactor;

    private static class Node<K, V> {
        final int hash;     // заранее вычисленный hash ключа
        final K key;        // final ключ, потому что ключ не должен меняться после вставки
        V value;            // изменчивое значение
        Node<K, V> next;    // ссылка на следующий узел в бакате для цепочки коллизий

        public Node(int hash, K key, V value, Node<K, V> next) {
            this.hash = hash;
            this.key = key;
            this.value = value;
            this.next = next;
        }

        // методы
        public K getKey() { return key; }

        public V getValue() { return value; }

        public final V setValue(V newValue) {
            V oldValue = value;
            value = newValue;
            return oldValue;
        }
    }

    // конструкторы
    public MyHashMap() {
        this.loadFactor = DEFAULT_LOAD_FACTOR;
    }

    public MyHashMap(int initCap) {
        this(initCap, DEFAULT_INIT_CAP);
    }

    public MyHashMap(int initCap, float loadFactor) {
        if (initCap < 0) {
            throw new IllegalArgumentException("Illegal initCap: " + initCap);
        }

        if (initCap > MAXIMUM_CAPACITY)
            initCap = MAXIMUM_CAPACITY;

        if (loadFactor <= 0 || Float.isNaN(loadFactor)) {
            throw new IllegalArgumentException("Illegal loadFactor: " + loadFactor);
        }

        this.loadFactor = loadFactor;
        this.threshold = tableSizeFor(initCap);
    }

    // количество сохранённых пар
    public int size() { return size;  }

    // проверка на пустой size
    public boolean isEmpty(){ return size == 0; }

    // взвращает ближайшую степень двойки для числа >= capacity, чтобы ёмкость таблицы была степень двойки (100 -> 128)
    private static int tableSizeFor(int cap) {
        int n = -1 >>> Integer.numberOfLeadingZeros(cap - 1);
        return (n < 0) ? 1 : (n >= MAXIMUM_CAPACITY) ? MAXIMUM_CAPACITY : n + 1;
    }

    // вычисление hash ключа, null-ключ поддерживается и имеет hash = 0, старшие биты перемешиваются в младшие
    private static int hash(Object key) {
        int h;
        return (key == null) ? 0 : (h = key.hashCode()) ^ (h >>> 16);
    }

    // сравнение ключей с поддержкой null
    // нужен, чтобы в get(), put() и remove() сравнивать сохранённый ключ с искомым, не падая с NullPointerException, если один из них null
    private static boolean keyEquals(Object k1, Object k2){
        return Objects.equals(k2, k1);
    }

    /*
         добавляет пару ключ-значение
         если ключ уже есть - заменяет значение и возвращает старое
         если ключа не было - возвращает null

         вычисляет хеш ключа, при необходимости создаёт массив бакетов, находит бакет по индексу (n - 1) & hash,
         ищет ключ в цепочке коллизий по паре (hash, equals), при совпадении заменяет значение и возвращает старое,
         иначе вставляет новый узел в начало бакета, увеличивает size и при превышении threshold расширяет таблицу
     */
    public V put(K key, V value){
        int hash = hash(key);

        // при первом put создаём table
        if (table == null || table.length == 0){
            resize();
        }

        int tableLength = table.length;
        int index = (tableLength - 1) & hash; // вычисляет индекс бакета

        Node<K,V> listHead = table[index]; // голова односвязного списка в этом бакете. Может быть null, если бакет пустой, или указывать на первый узел цепочки коллизий

        // идём по цепочке и ищем существующий ключ в бакете
        for (Node<K,V> element = listHead; element != null; element = element.next) {
            if (element.hash == hash && keyEquals(element.key, key)){
                V oldValue = element.value;
                element.value = value;
                return oldValue;
            }
        }

        // ключа нет - вставляем новый узел в начало бакета
        Node<K,V> newNode = new Node<>(hash, key, value, listHead);

        // новые элементы оказываются впереди старых, и при частых обращениях к старым ключам они ищутся дольше
        table[index] = newNode; // newNode становится новой головой бакета

        // увеличиваем size и при необходимости расширяем table
        if (++size > threshold){
            resize();
        }
        return null;
    }

    // возвращает значение по ключу или null, если ключа нет
    public V get(Object key){
        if (table == null || table.length == 0){
            return null;
        }

        int hash = hash(key);
        int index = (table.length - 1) & hash; // вычисляет индекс бакета

        // идём по цепочке в бакете
        for (Node<K,V> element = table[index]; element != null; element = element.next) {
            if (element.hash == hash && keyEquals(element.key, key)){
                return element.value;
            }
        }
        return null; // null может означать и отсутствие ключа, и сохранённое null-значение
    }

    // удаляет пару по ключу, возвращает удалённое значение или null, если ключа не было
    public void remove(Object key){
        if (table == null || table.length == 0){
            return;
        }

        int hash = hash(key);
        int index = (table.length - 1) & hash; // вычисляет индекс бакета

        Node<K,V> prevElem = null;
        Node<K,V> element = table[index];

        // идёт по цепочке, храня предыдущий узел в prevElem
        while (element != null){
            if (element.hash == hash && keyEquals(element.key, key)){
                if (prevElem == null){ // если удаляемый узел первый в бакете: table[index] = element.next;
                    table[index] = element.next;
                } else {
                    prevElem.next = element.next;
                }
                size--;
                return;
            }
            prevElem = element;
            element = element.next;
        }
    }

    // расширяет table(массив бакетов) в 2 раза и перераспределяет все узлы
    // resize() создаёт новый массив, проходит по всем старым бакетам и переносит каждый узел в новый бакет по индексу (newCap - 1) & hash.
    private void resize(){

        Node<K,V>[] oldTable = table; // сохраняем старую таблицу
        int oldCap = (oldTable == null) ? 0 : oldTable.length;
        int oldThreshold = threshold; // сохраняем старый threshold

        int newCap;
        int newThreshold = 0;

        /*
        если таблица уже была - oldCap << 1;
        если это первый resize - берём threshold, который хранил начальную ёмкость;
        иначе DEFAULT_INIT_CAP.
         */
        if (oldCap > 0){
            if (oldCap >= MAXIMUM_CAPACITY){ // если таблица уже >= MAXIMUM_CAPACITY, дальше расширять нельзя (иначе переполнение int или OutOfMemoryError)
                threshold = Integer.MAX_VALUE;
                return;
            }

            // когда внутренний массив коллекции заполняется, его размер увеличивается вдвое
            newCap = oldCap << 1; // можно сделать oldCap * 2
            newThreshold = (int) (newCap*loadFactor); // 32 * 0,75 = 24

        } else if (oldThreshold > 0) {
            // первый resize: threshold хранил начальную ёмкость
            newCap = oldThreshold;
            newThreshold = (int) (newCap*loadFactor);

        } else {
            // конструктор по умолчанию если без аргументов создали мапу
            newCap = DEFAULT_INIT_CAP;
            newThreshold = (int) (DEFAULT_INIT_CAP * DEFAULT_LOAD_FACTOR);
        }

        // пересчёт threshold если он остался 0
        if (newThreshold == 0){ // защита от усечения до нуля при маленьких значениях
            float floatThreshold = (float) newCap * loadFactor;
            newThreshold = (newCap < MAXIMUM_CAPACITY && floatThreshold < (float)MAXIMUM_CAPACITY) ? (int) floatThreshold : Integer.MAX_VALUE;
        }

        threshold = newThreshold; // фиксируем новый порог в поле класса. Именно с этим значением будет сравниваться size в put()

        @SuppressWarnings({"rawtypes","unchecked"}) // для массива
        Node<K,V>[] newTable = (Node<K,V>[]) new Node[newCap]; // создаём новый массив бакетов длины newCap. Все ячейки null. [ПРИВЕДЕНИЕ ТИПА - В Java нельзя создать массив обобщённого типа напрямую (new Node<K,V>[newCap] не скомпилируется из-за стирания типов]
        table = newTable; // поле table начинает указывать на новый массив. Старый остаётся доступным через oldTable. Именно из-за этой строки мы сохраняли oldTable

        // перенос элементов из старого массива в новый
        if (oldTable != null){
            // проходим по всем старым бакетам и переносим каждый узел в новый бакет по индексу
            for (int j = 0; j < oldCap; j++) {
                Node<K,V> element = oldTable[j];

                while (element != null){
                    Node<K,V> next = element.next;

                    int index = (newCap - 1) & element.hash;

                    // вставляем в начало нового бакета
                    element.next = newTable[index]; // новый узел указывает на текущую голову новой корзины (может быть null, если корзина пустая)
                    newTable[index] = element; //  новый узел становится новой головой
                    element = next; // Берём сохранённый next и продолжаем обход. Когда element станет null, while завершится, и внешний for перейдёт к следующему бакету
                }
            }
        }
    }
}
