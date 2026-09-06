# Market Notifier

[![CI](https://github.com/juninatt/telegram-market-notifier/actions/workflows/ci.yml/badge.svg)](https://github.com/juninatt/telegram-market-notifier/actions/workflows/ci.yml)
[![CodeQL](https://github.com/juninatt/telegram-market-notifier/actions/workflows/codeql.yml/badge.svg)](https://github.com/juninatt/telegram-market-notifier/actions/workflows/codeql.yml)

**Market Notifier** is a personal Java project built with Maven and Spring Boot.
It integrates multiple financial data providers (e.g., [Finnhub](https://finnhub.io/), [Marketaux](https://www.marketaux.com/))
and delivers automated, scheduled updates through [Telegram](https://telegram.org/) and/or email.

Users interact with the application via the [Telegram Bot API](https://core.telegram.org/bots/api), where they can create and manage news subscriptions.
By sending simple commands such as `/subscribe <keyword>` or `/list`, the bot allows users to follow specific topics and receive financial news updates directly in Telegram.

For a more detailed usage guide, including full `/subscribe` syntax, examples, and parameter rules, see the [How to Use guide](_docs/how-to-use.md).

### Market data providers

- **Finnhub**
Financial data API that offers a wide range of information, including general business news.

- **Marketaux**
Financial news API focused on delivering headline articles filtered by criteria like company, region, or language.

### Delivery channels

- **Telegram** — interactive; subscriptions are created and managed via bot commands.
- **Email** — delivered via [Resend](https://resend.com); currently configured directly in the subscriptions file
(there's no `/subscribe` support for setting an email address yet — see Configuration below).

A subscription can use either channel, both, or neither (if disabled) — the dispatcher resolves whichever address each registered channel needs per subscription.

---

## ✅ Requirements

Java 17+, Maven, and accounts for [Finnhub](https://finnhub.io), [Marketaux](https://marketaux.com), [Telegram](https://core.telegram.org/bots/api), and — if you want email delivery — [Resend](https://resend.com).

---

## 🧩 Project structure

```text
telegram-market-notifier/
 ├── app-runner/         # Application entry point and global configuration
 ├── core/               # Shared domain models (NewsItem, NewsGroup, Notification, SchedulePreset, ...)
 ├── sources/            # Integrations for external financial news APIs (Finnhub, Marketaux)
 ├── subscription/       # Subscription storage, validation, and formatting
 ├── telegram/           # Telegram integration, bot commands, and message delivery
 ├── email/              # Email delivery via Resend
 └── dispatch/           # Ties schedules, sources, filtering, grouping, and delivery together
```

---

## ⚙️ Configuration

Before running the application, make sure you have valid API tokens for **Finnhub**, **Marketaux**, a **Telegram bot token**, and — for email delivery — a **Resend API key**.

### 1) Get API keys / tokens

**Finnhub / Marketaux (news sources)**
- Sign up to get free API keys:
- [finnhub.io](https://finnhub.io)
- [marketaux.com](https://marketaux.com)

**Telegram**
1. In Telegram, start a chat with **@BotFather**.
2. Send `/newbot`, follow the prompts (choose a name and a unique username ending in `bot`).
3. BotFather will reply with your **bot token** — keep it secret.
4. Start a chat with your new bot so it can message you back.
5. (Optional: for group delivery) add your bot to the group and send a message in the group.

**Find your chat ID(s)**
- Quick way: call `getUpdates` and read the `chat.id` from the response.
```bash
  curl -s "https://api.telegram.org/bot${TELEGRAM_BOT_TOKEN}/getUpdates"
```

**Resend (email delivery)**
1. Sign up at [resend.com](https://resend.com) (free tier: 3,000 emails/month).
2. Verify a sending domain, or use Resend's shared test sender while developing.
3. Create an API key.

### 2) Configure application.yml

A central `application.yml` in `app-runner/resources` loads separate YAML files for each module.
**Configuration file structure:**

```yaml
app-runner/
└── src/main/resources/
├── application.yml
├── application-finnhub.yml
├── application-marketaux.yml
├── application-telegram.yml
├── application-subscription.yml
└── application-email.yml
```

Each file contains placeholders for its own API tokens and settings, read from environment variables:
`FINNHUB_API_KEY`, `MARKETAUX_API_KEY`, `TELEGRAM_BOT_TOKEN`, `TELEGRAM_CHAT_IDS`, `RESEND_API_KEY`, `RESEND_FROM_ADDRESS`.
All configuration files are loaded automatically when the application starts.

### 3) Enable email delivery for a subscription

There's no bot command for this yet — add an `email` field directly to the subscription's entry in the subscriptions file (path set by `subscription.storage.path`, default `subscriptions.yml`):

```yaml
subscriptions:
- id: "sub-1"
  chatId: 123456789
  email: "you@example.com"
  schedule: MORNING
  filter:
    keywords: ["Tesla"]
    tickers: ["TSLA"]
    language: "en"
  maxItems: 10
  enabled: true
```

Leave `email` unset (or `null`) to only deliver via Telegram for that subscription.

---

## 🗓️ Delivery schedule

Each subscription picks a `schedule` from a fixed set of presets, each evaluated in its own timezone:

| Preset                  | Fires                                  | Timezone            |
|--------------------------|-----------------------------------------|----------------------|
| `MORNING`                 | 08:00 daily                             | Europe/Stockholm    |
| `EVENING`                 | 20:00 daily                             | Europe/Stockholm    |
| `MORNING_EVENING`         | 08:00 and 20:00 daily                   | Europe/Stockholm    |
| `MORNING_LUNCH_EVENING`   | 08:00, 12:00, and 20:00 daily           | Europe/Stockholm    |
| `EUROPE_MARKET_OPEN`      | 09:00, weekdays                         | Europe/Stockholm    |
| `EUROPE_MARKET_CLOSE`     | 17:30, weekdays                         | Europe/Stockholm    |
| `US_MARKET_OPEN`          | 09:30, weekdays                         | America/New_York    |
| `US_MARKET_CLOSE`         | 16:00, weekdays                         | America/New_York    |

The market-hour presets only fire on weekdays and are evaluated in the market's own timezone, so they land correctly even when US and European daylight saving transitions fall on different dates.

---

## ▶️ Run

To run the application, first build all modules and generate the necessary artifacts using Maven.

From the project root:
```bash
    mvn clean install
```

Once the build is complete, start the application with Spring Boot:
```bash
    mvn spring-boot:run -pl app-runner
```

This will:

1. Load the main configuration from `app-runner/resources/application.yml`.
2. Import module-specific configurations for Telegram, Finnhub, Marketaux, subscriptions, and email.
3. Initialize all services and start the news dispatch scheduler, which checks every minute for a due schedule preset, fetches and groups news from all sources, and delivers matching items to each subscription's configured channels.

---

## 💻 Development Notes

Lombok is used in this project so if you're using an IDE, make sure annotation processing is enabled in your settings.
No additional setup is needed when building or running from the command line with Maven.
