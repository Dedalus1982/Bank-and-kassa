## 1. Коротко
Код представляет собой многопоточную модель банка, в которой клиенты могут пополнять свои счета, снимать деньги и переводить средства между собой. Кассиры обрабатывают транзакции в отдельных потоках, а ScheduledExecutorService используется для выполнения периодических задач. Код также использует потокобезопасные структуры данных, что минимизирует риски гонок данных и обеспечивает целостность данных
На пуске программа попросит ввести количество клиентов банка и все, далее - симуляцие переводов, поплнений и выводов денег.

### Описание модели
## 1. Класс `Client`
Клиента банка (островитянин), содержит информацию о его идентификаторе и балансах в различных валютах.
- **Поля:**
  - `id`: уникальный идентификатор клиента.
  - `balances`: карта, хранящая балансы клиента по валютам (RaiStone, USD, EUR).

- **Конструктор:**
  - Инициализирует идентификатор клиента и устанавливает начальные балансы.

- **Методы:**
  - `getId()`: возвращает идентификатор клиента.
  - `getBalance(String currency)`: возвращает баланс клиента в указанной валюте.
  - `deposit(String currency, double amount)`: пополняет баланс клиента в заданной валюте. Метод синхронизирован для предотвращения гонок данных.
  - `withdraw(String currency, double amount)`: снимает сумму с баланса клиента, если достаточно средств. Также синхронизирован.
  - `transfer(String currency, double amount)`: добавляет сумму на баланс клиента в указанной валюте. Метод синхронизирован.

## 2. Класс `Bank`
Управляет клиентами, кассирами и транзакциями.

- **Поля:**
  - `clients`: потокобезопасная карта для хранения клиентов.
  - `transactionQueue`: очередь для хранения транзакций, которые должны быть выполнены кассирами.
  - `cashiers`: список кассиров.
  - `observers`: список наблюдателей (например, логгеров).
  - `currencyRates`: карта для хранения курсов валют.

- **Конструктор:**
  - Создает заданное количество кассиров и инициализирует курсы валют.

- **Методы:**
  - `addObserver(Observer observer)`: добавляет нового наблюдателя.
  - `notifyObservers(String message)`: уведомляет всех наблюдателей о событиях.
  - `addClient(Client client)`: добавляет нового клиента в банк.
  - `queueTransaction(Runnable transaction)`: добавляет транзакцию в очередь.
  - `takeTransaction()`: извлекает и возвращает транзакцию из очереди.
  - `getClient(int id)`: возвращает клиента по идентификатору.
  - `displayBalances()`: выводит балансы всех клиентов и текущие курсы валют.
  - `updateCurrencyRate(String pair, double newRate)`: обновляет курс валюты и уведомляет наблюдателей.

### 5. Класс `Cashier`

Класс `Cashier` наследует `Thread` и представляет кассира, который обрабатывает транзакции.

- **Методы:**
  - `run()`: основной метод, выполняющийся в потоке кассира. Он извлекает транзакции из очереди и выполняет их.
  - `deposit(int clientId, String currency, double amount)`: создает транзакцию пополнения счета клиента.
  - `withdraw(int clientId, String currency, double amount)`: создает транзакцию снятия средств.
  - `transferFunds(int senderId, int receiverId, String currency, double amount)`: создает транзакцию перевода средств между клиентами.
### 2. Интерфейс `Observer`

Этот интерфейс определяет метод `update(String message)`, который будет реализован классами, желающими получать уведомления о событиях (например, логгирования).

### 3. Класс `Logger`

Этот класс реализует интерфейс `Observer` и отвечает за логирование сообщений. Метод `update` выводит сообщения в консоль.


### 6. Класс `BankOfYapIslands`

Основной класс для запуска приложения.

- **Методы:**
  - `main(String[] args)`: основной метод, который:
    - Запрашивает количество клиентов.
    - Создает экземпляр `Bank` и добавляет клиентов с случайными начальными балансами.
    - Настраивает `ScheduledExecutorService` для периодического выполнения различных задач:
      - Выполнение случайных транзакций каждые 3 секунды.
      - Обновление курсов валют каждые 10 секунд.
      - Периодическое пополнение баланса клиентов каждые 45 секунд.
      - Отображение балансов каждые 30 секунд.
    - Добавляет хук завершения, чтобы корректно завершить работу `executorService` при завершении программы.

### Заключение

Код представляет собой многопоточную модель банка, в которой клиенты могут пополнять свои счета, снимать деньги и переводить средства между собой. Кассиры обрабатывают транзакции в отдельных потоках, а `ScheduledExecutorService` используется для выполнения периодических задач. Код также использует потокобезопасные структуры данных, что минимизирует риски гонок данных и обеспечивает целостность данных.
