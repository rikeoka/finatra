package com.twitter.finatra.tests.utils

import com.twitter.finatra.utils.FutureUtils
import com.twitter.finatra.utils.FutureUtils._
import com.twitter.inject.Test
import com.twitter.util.{Await, Future, FuturePool}
import java.util.concurrent.{ConcurrentLinkedQueue, Executors}
import scala.collection.JavaConverters._

class FutureUtilsTest extends Test {
  val ActionLog = new ConcurrentLinkedQueue[String]
  val TestFuturePool = FuturePool(Executors.newFixedThreadPool(4))

  override def beforeEach {
    ActionLog.clear()
  }

  "FutureUtils" should {
    "#sequentialMap" in {
      assertFuture(
        sequentialMap(Seq(1, 2, 3))(mockSvcCall),
        Future(Seq("1", "2", "3")))

      ActionLog.asScala.toSeq should equal(Seq("S1", "E1", "S2", "E2", "S3", "E3"))
    }
  }

  "#collectMap" in {
    val result = Await.result(
      collectMap(Seq(1, 2, 3))(mockSvcCall))
    result.sorted should equal(Seq("1", "2", "3"))

    ActionLog.asScala.toSeq.sorted should equal(Seq("S1", "E1", "S2", "E2", "S3", "E3").sorted)
  }

  "success future" in {
    val f = Await.result(FutureUtils.exceptionsToFailedFuture {
      Future("hi")
    })
    f should equal("hi")
  }

  "failed future" in {
    val e = intercept[Exception] {
      Await.result(FutureUtils.exceptionsToFailedFuture {
        Future.exception(new Exception("failure"))
      })
    }
    e.getMessage should equal("failure")
  }

  "thrown exception" in {
    val e = intercept[Exception] {
      Await.result(FutureUtils.exceptionsToFailedFuture {
        throw new Exception("failure")
      })
    }
    e.getMessage should equal("failure")
  }

  def mockSvcCall(num: Int): Future[String] = {
    ActionLog add ("S" + num)
    TestFuturePool {
      Thread.sleep(250)
      ActionLog add ("E" + num)
      num.toString
    }
  }
}
