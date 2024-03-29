package Producer

import java.util.Properties

import com.danielasfregola.twitter4s.TwitterStreamingClient
import com.danielasfregola.twitter4s.entities.Tweet
import com.typesafe.config.Config
import org.apache.kafka.clients.producer._

import scala.collection.JavaConverters._

class TwitterProducer(conf: Config) extends Runnable {
  def run = {

    val props = new Properties()
    props.put("bootstrap.servers", conf.getString("kafkaServer"))
    props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer")
    props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer")

    val producer = new KafkaProducer[String, String](props)
    val topic = conf.getString("inputTopic")

    val streamingClient = TwitterStreamingClient()
    val trackedWords = conf.getStringList("twitter.keywords").asScala.toList

    streamingClient.filterStatuses(tracks = trackedWords) {
      case tweet: Tweet =>
        val epoch = tweet.created_at.getEpochSecond
        val text = tweet.text
        val record = new ProducerRecord(topic, epoch.toString, text)
        producer.send(record)
    }
    sys.ShutdownHookThread {
      producer.close()
    }
  }
}
