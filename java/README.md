# ANA Messaging Java SDK

Official ANA Messaging SDK for Java 11+. Send WhatsApp and SMS through the ANA Messaging API from
Spring Boot, Jakarta EE, desktop (Swing/JavaFX), Android-free JVM tools and ERP connectors.

You need a **client id and secret** from anamessaging.com (Settings → API Keys) and an **active API
subscription**. Without the subscription, calls throw `SubscriptionInactiveException`.

## Install

**Maven**

```xml
<dependency>
  <groupId>com.anamessaging</groupId>
  <artifactId>anamessaging-sdk</artifactId>
  <version>1.0.0</version>
</dependency>
```

**Gradle**

```groovy
implementation 'com.anamessaging:anamessaging-sdk:1.0.0'
```

**Plain JAR (desktop apps, no build tool):** `mvn package` produces
`target/anamessaging-sdk-1.0.0-all.jar`. It bundles a relocated Jackson, so put this one JAR on
the classpath and you're done.

## Quick start

```java
import com.anamessaging.sdk.AnaMessagingClient;
import com.anamessaging.sdk.model.*;
import com.anamessaging.sdk.exception.*;

AnaMessagingClient ana = AnaMessagingClient.builder()
        .clientId(System.getenv("ANAMESSAGING_CLIENT_ID"))
        .clientSecret(System.getenv("ANAMESSAGING_CLIENT_SECRET"))
        // .sandbox()            // https://api-dev.anamessaging.com
        .build();                // or AnaMessagingClient.fromEnv()

// Text
ana.whatsapp().sendText("919876543210", "Your order has shipped.");

// Template: "Hi {{1}}, order {{2}} is confirmed."
ApiResponse<MessageDispatchResult> res = ana.whatsapp().send(
        SendWhatsAppRequest.template("919876543210", "order_confirmation")
                .language("en")
                .components(TemplateComponent.body("Ravi", "SO-1042"))
                .campaignId("SO-1042"));
System.out.println(res.data.messageId + " " + res.data.status);   // queued

// Template with a document header (invoice PDF)
ana.whatsapp().send(
        SendWhatsAppRequest.template("919876543210", "invoice_document").components(TemplateComponent.body("INV-001")),
        FileAttachment.fromPath(Path.of("INV-001.pdf")));

// Media
ana.whatsapp().send(SendWhatsAppRequest.mediaUrl("919876543210", "image", "https://cdn.example.com/p.jpg").caption("New stock"));
long id = ana.media().upload(FileAttachment.fromPath(Path.of("catalogue.pdf")), null).data.mediaUploadId;
ana.whatsapp().send(SendWhatsAppRequest.mediaUpload("919876543210", id).filename("catalogue.pdf"));

// SMS
ana.sms().send(SendSmsRequest.to("919876543210", "Your OTP is 481920")
        .senderId("ANATEC").dlt("1101234567890123456", "1107161234567890123"));
ana.sms().send(SendSmsRequest.toMany("Store closed today", "919876543210", "919812345678"));

// Bulk and campaigns
ana.whatsapp().sendBulk(SendWhatsAppBulkRequest.template("diwali_offer").recipients("919876543210", "919812345678"));
String campaignId = ana.campaigns().run(SendWhatsAppBulkRequest.template("diwali_offer").name("Diwali - Delhi")).data.campaignId;
CampaignDetail detail = ana.campaigns().get(campaignId).data;
```

## Exceptions (unchecked)

`AnaMessagingException` (base: `getStatus()`, `getCode()`, `getErrors()`), then
`ValidationException` (400), `AuthenticationException` (401), `PermissionDeniedException` (403),
`SubscriptionInactiveException` (403, no active subscription), `NotFoundException` (404),
`RateLimitException` (429, `getRetryAfterSeconds()`), `QuotaExceededException` (429 quota),
`ServerException` (5xx), `ConnectionException`.

The client is thread-safe. Build it once (for example as a Spring `@Bean`) and share it.

```java
@Bean
AnaMessagingClient anaMessaging(@Value("${anamessaging.client-id}") String id,
                                @Value("${anamessaging.client-secret}") String secret) {
    return AnaMessagingClient.builder().clientId(id).clientSecret(secret).build();
}
```

## Build and publish

```bash
mvn package     # tests + jar, -sources, -javadoc, -all (fat) jars in target/
```

Publishing: add a `<distributionManagement>` for your repository (Maven Central via the Sonatype
Central Portal with GPG signing, GitHub Packages, Nexus or Artifactory) and run `mvn deploy`. The
repository workflow publishes to GitHub Packages:
`mvn deploy -DaltDeploymentRepository=github::https://maven.pkg.github.com/anatech-prod/anamessaging-apps`.
