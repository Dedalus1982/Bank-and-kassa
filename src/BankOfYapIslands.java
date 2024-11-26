import java.util.*;
import java.util.concurrent.*;

// Класс, представляющий клиента банка
class Client {
    private final int id;
    private final Map<String, Double> balances;

    public Client(int id, double stones, double dollars, double euros) {
        this.id = id;
        balances = new HashMap<>();
        balances.put("RaiStone", stones);
        balances.put("USD", dollars);
        balances.put("EUR", euros);
    }

    public int getId() {
        return id;
    }

    public double getBalance(String currency) {
        return balances.getOrDefault(currency, 0.0);
    }

    public synchronized void deposit(String currency, double amount) {
        balances.put(currency, balances.getOrDefault(currency, 0.0) + amount);
    }

    public synchronized boolean withdraw(String currency, double amount) {
        if (balances.getOrDefault(currency, 0.0) >= amount) {
            balances.put(currency, balances.get(currency) - amount);
            return true;
        }
        return false;
    }

    public synchronized void transfer(String currency, double amount) {
        balances.put(currency, balances.getOrDefault(currency, 0.0) + amount);
    }
}

// Интерфейс для паттерна Observer
interface Observer {
    void update(String message);
}

// Реализация логгирования
class Logger implements Observer {
    @Override
    public void update(String message) {
        System.out.printf("Log: %s%n", message);
    }
}

// Основной класс банка
class Bank {
    private final ConcurrentHashMap<Integer, Client> clients = new ConcurrentHashMap<>();
    private final Queue<Runnable> transactionQueue = new LinkedBlockingQueue<>();
    private final List<Cashier> cashiers = new ArrayList<>();
    private final List<Observer> observers = new ArrayList<>();
    private final Map<String, Double> currencyRates = new HashMap<>();

    public Bank(int cashierCount) {
        for (int i = 0; i < cashierCount; i++) {
            Cashier cashier = new Cashier(this);
            cashiers.add(cashier);
            cashier.start();
        }
        // Инициализация курсов валют
        currencyRates.put("RaiStone/USD", 0.15);
        currencyRates.put("RaiStone/EUR", 0.30);
        currencyRates.put("EUR/USD", currencyRates.get("RaiStone/USD") / currencyRates.get("RaiStone/EUR")); // Кросс-курс
    }

    public List<Cashier> getCashiers() {
        return cashiers;
    }

    public Cashier getFirstCashier() {
        return cashiers.isEmpty() ? null : cashiers.get(0);
    }

    public void addObserver(Observer observer) {
        observers.add(observer);
    }

    public void notifyObservers(String message) {
        for (Observer observer : observers) {
            observer.update(message);
        }
    }

    public void addClient(Client client) {
        clients.put(client.getId(), client);
    }

    public void queueTransaction(Runnable transaction) {
        transactionQueue.offer(transaction);
    }

    public Runnable takeTransaction() throws InterruptedException {
        return transactionQueue.poll();
    }

    public Client getClient(int id) {
        return clients.get(id);
    }

    public void displayBalances() {
        System.out.println("Баланс банка Острова Яп:");
        for (Client client : clients.values()) {
            System.out.printf("Островитянин %d: RaiStone: %.2f, USD: %.2f, EUR: %.2f%n",
                    client.getId(),
                    client.getBalance("RaiStone"),
                    client.getBalance("USD"),
                    client.getBalance("EUR"));
        }

        // Показать текущие курсы валют
        System.out.println("Текущие курсы валют:");
        for (Map.Entry<String, Double> entry : currencyRates.entrySet()) {
            System.out.printf("%s: %.4f%n", entry.getKey(), entry.getValue());
        }
    }

    public void updateCurrencyRate(String pair, double newRate) {
        currencyRates.put(pair, newRate);
        notifyObservers("Курс " + pair + " изменился на " + newRate);
        System.out.printf("Курс %s изменился на %.4f%n", pair, newRate);

        // Обновить кросс-курс EUR/USD
        if (pair.equals("RaiStone/USD") || pair.equals("RaiStone/EUR")) {
            currencyRates.put("EUR/USD", currencyRates.get("RaiStone/USD") / currencyRates.get("RaiStone/EUR"));
            notifyObservers("Курс EUR/USD обновлен на " + currencyRates.get("EUR/USD"));
        }
    }
}

// Класс, представляющий кассу
class Cashier extends Thread {
    private final Bank bank;

    public Cashier(Bank bank) {
        this.bank = bank;
    }

    @Override
    public void run() {
        while (true) {
            try {
                Runnable transaction = bank.takeTransaction();
                if (transaction != null) {
                    transaction.run();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }

    public void deposit(int clientId, String currency, double amount) {
        bank.queueTransaction(() -> {
            Client client = bank.getClient(clientId);
            synchronized (client) { // Синхронизация на уровне клиента
                client.deposit(currency, amount);
                bank.notifyObservers("Островитянин " + clientId + " пополнился " + Math.round(amount) + " " + currency);
            }
        });
    }

    public void withdraw(int clientId, String currency, double amount) {
        bank.queueTransaction(() -> {
            Client client = bank.getClient(clientId);
            synchronized (client) { // Синхронизация на уровне клиента
                if (client.withdraw(currency, amount)) {
                    bank.notifyObservers("Островитянин " + clientId + " снял сумму " + Math.round(amount) + " " + currency);
                } else {
                    bank.notifyObservers("Островитянин " + clientId + " не смог снять сумму " + Math.round(amount) + " " + currency + " из-за недостатка средств.");
                }
            }
        });
    }

    public void transferFunds(int senderId, int receiverId, String currency, double amount) {
        bank.queueTransaction(() -> {
            Client sender = bank.getClient(senderId);
            Client receiver = bank.getClient(receiverId);
            synchronized (sender) { // Синхронизация на уровне отправителя
                if (sender.withdraw(currency, amount)) {
                    synchronized (receiver) { // Синхронизация на уровне получателя
                        receiver.transfer(currency, amount);
                        bank.notifyObservers("Островитянин " + senderId + " перевел " + Math.round(amount) + " " + currency + " Островитянину " + receiverId);
                    }
                } else {
                    bank.notifyObservers("Островитянин " + senderId + " не перевел " + Math.round(amount) + " " + currency + " из-за недостатка средств.");
                }
            }
        });
    }
}

// Главный класс для запуска приложения
public class BankOfYapIslands {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);

        // Запрос количества клиентов
        System.out.print("Введите количество островитян: ");
        int clientCount = scanner.nextInt();

        Bank bank = new Bank(3); // 3 кассира
        Logger logger = new Logger();
        bank.addObserver(logger);

        // Добавление клиентов с начальными балансами
        for (int i = 1; i <= clientCount; i++) {
            double initialRaiStones = 1000 + Math.random() * 1000; // Случайный баланс в рублях
            double initialDollars = 500 + Math.random() * 500; // Случайный баланс в долларах
            double initialEuros = 300 + Math.random() * 300; // Случайный баланс в евро
            bank.addClient(new Client(i, initialRaiStones, initialDollars, initialEuros));
        }

        ScheduledExecutorService executorService = Executors.newScheduledThreadPool(1);

        // Выполнение транзакций каждые 3 секунды
        executorService.scheduleAtFixedRate(() -> {
            Cashier cashier = bank.getFirstCashier(); // Use the new method
            if (cashier != null) { // Check if cashier is not null
                int senderId = 1 + new Random().nextInt(clientCount);
                int receiverId = 1 + new Random().nextInt(clientCount);
                String currency = new Random().nextBoolean() ? "RaiStone" : (new Random().nextBoolean() ? "USD" : "EUR");
                double amount = 50 + new Random().nextDouble() * 50; // Случайная сумма от 50 до 100
                cashier.transferFunds(senderId, receiverId, currency, amount);
            }
        }, 0, 3, TimeUnit.SECONDS);

        // Обновление курсов валют каждые 30 секунд
        executorService.scheduleAtFixedRate(() -> {
            // Пример изменения курса
            double newRateUSD = 0.15 + Math.random() * 0.1; // Случайное значение для RUB/USD
            bank.updateCurrencyRate("RaiStone/USD", newRateUSD);

            double newRateEUR = 0.30 + Math.random() * 0.1; // Случайное значение для RUB/EUR
            bank.updateCurrencyRate("RaiStone/EUR", newRateEUR);
        }, 0, 10, TimeUnit.SECONDS);

        // Периодическое пополнение баланса клиентов каждые 45 секунд
        executorService.scheduleAtFixedRate(() -> {
            for (int i = 1; i <= clientCount; i++) {
                Client client = bank.getClient(i);
                if (client != null) {
                    client.deposit("RaiStone", 1000);
                    bank.notifyObservers("Островитянин " + i + " пополнил свой счет на 1000 RaiStone");
                }
            }
        }, 0, 45, TimeUnit.SECONDS);

        // Отображение балансов каждые 30 секунд
        executorService.scheduleAtFixedRate(bank::displayBalances, 0, 30, TimeUnit.SECONDS);

        // завершения программы
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            executorService.shutdown();
            try {
                executorService.awaitTermination(1, TimeUnit.MINUTES);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }));

        scanner.close();
    }
}
