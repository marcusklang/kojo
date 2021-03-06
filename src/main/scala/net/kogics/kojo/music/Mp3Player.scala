/*
 * Copyright (C) 2011 Lalit Pant <pant.lalit@gmail.com>
 *
 * The contents of this file are subject to the GNU General Public License
 * Version 3 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of
 * the License at http://www.gnu.org/copyleft/gpl.html
 *
 * Software distributed under the License is distributed on an "AS
 * IS" basis, WITHOUT WARRANTY OF ANY KIND, either express or
 * implied. See the License for the specific language governing
 * rights and limitations under the License.
 *
 */

package net.kogics.kojo
package music

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock

import javax.swing.Timer

import net.kogics.kojo.core.KojoCtx

import javazoom.jl.player.Player
import util.AsyncQueuedRunner
import util.Utils
import util.Utils.giveupLock
import util.Utils.withLock

trait Mp3Player {
  val pumpEvents: Boolean
  val kojoCtx: KojoCtx
  def showError(msg: String)
  lazy private val listener = kojoCtx.activityListener

  @volatile private var mp3Player: Option[Player] = None
  @volatile private var bgmp3Player: Option[Player] = None
  private val playLock = new ReentrantLock
  private val stopped = playLock.newCondition
  private var stopBg = false
  private var stopFg = false
  private var timer: Timer = _

  private def startPumpingEvents() {
    if (pumpEvents && timer == null) {
      listener.hasPendingCommands()
      timer = Utils.scheduleRec(0.5) {
        listener.hasPendingCommands()
      }
    }
  }

  private def stopPumpingEvents() {
    if (pumpEvents && bgmp3Player.isEmpty) {
      if (timer != null) {
        timer.stop()
        timer = null
      }
      listener.pendingCommandsDone()
      Utils.schedule(0.5) {
        listener.pendingCommandsDone()
      }
    }
  }

  private def playHelper(fname: String)(fn: (InputStream) => Unit) {
    val is = getClass.getResourceAsStream(fname)
    if (is != null) {
      fn(is)
    }
    else {
      val mp3File = Utils.absolutePath(fname)
      val f = new File(mp3File)
      if (f.exists) {
        val is = new FileInputStream(f)
        fn(is)
        //      is.close() - player closes the stream
      }
      else {
        showError("MP3 file does not exist - %s" format (f.getAbsolutePath))
      }
    }
  }

  def isMp3Playing: Boolean = {
    withLock(playLock) {
      mp3Player.isDefined
    }
  }

  def playMp3(mp3File: String) {
    def done() {
      stopFg = false
      mp3Player = None
      stopPumpingEvents()
      stopped.signal()
    }
    withLock(playLock) {
      stopMp3()
      playHelper(mp3File) { is =>
        mp3Player = Some(new Player(is))
      }
      if (mp3Player.isDefined) {
        Utils.runAsync {
          withLock(playLock) {
            if (stopFg) {
              done()
            }
            else {
              val music = mp3Player.get
              giveupLock(playLock) {
                music.play()
              }
              done()
            }
          }
        }
        startPumpingEvents()
      }
    }
  }

  lazy val queuedRunner = new AsyncQueuedRunner {}
  def playMp3Sound(mp3File: String) {
    def done() {
      stopFg = false
      mp3Player = None
      stopped.signal()
    }
    withLock(playLock) {
      stopMp3()
      playHelper(mp3File) { is =>
        mp3Player = Some(new Player(is))
      }
      if (mp3Player.isDefined) {
        queuedRunner.runAsyncQueued {
          withLock(playLock) {
            if (stopFg) {
              done()
            }
            else {
              val music = mp3Player.get
              giveupLock(playLock) {
                music.play()
              }
              done()
            }
          }
        }
      }
    }
  }

  def playMp3Loop(mp3File: String) {

    def playLoop0() {
      Utils.runAsync {
        withLock(playLock) {
          if (stopBg) {
            stopBg = false
            bgmp3Player = None
            stopPumpingEvents()
            stopped.signal()
          }
          else {
            val music = bgmp3Player.get
            giveupLock(playLock) {
              music.play()
            }
            playHelper(mp3File) { is =>
              bgmp3Player = Some(new Player(is))
            }
            if (bgmp3Player.isDefined) {
              playLoop0()
            }
          }
        }
      }
    }

    withLock(playLock) {
      stopMp3Loop()
      playHelper(mp3File) { is =>
        bgmp3Player = Some(new Player(is))
      }
      if (bgmp3Player.isDefined) {
        playLoop0()
        startPumpingEvents()
      }
    }
  }

  def stopMp3() {
    withLock(playLock) {
      if (mp3Player.isDefined) {
        stopFg = true
        if (!mp3Player.get.isComplete) {
          mp3Player.get.close()
        }
        while (stopFg) {
          val signalled = stopped.await(20, TimeUnit.MILLISECONDS)
          if (!signalled) {
            try {
              if (!mp3Player.get.isComplete) {
                mp3Player.get.close()
              }
            }
            catch {
              case t: Throwable => // do nothing
            }
          }
        }
      }
    }
  }

  def stopMp3Loop() {
    withLock(playLock) {
      if (bgmp3Player.isDefined) {
        stopBg = true
        if (!bgmp3Player.get.isComplete) {
          bgmp3Player.get.close()
        }
        while (stopBg) {
          val signalled = stopped.await(20, TimeUnit.MILLISECONDS)
          if (!signalled) {
            try {
              if (!bgmp3Player.get.isComplete) {
                bgmp3Player.get.close()
              }
            }
            catch {
              case t: Throwable => // do nothing
            }
          }
        }
      }
    }
  }
}

class KMp3(val kojoCtx: KojoCtx) extends Mp3Player {
  val pumpEvents = true
  def showError(msg: String) = println(msg)
}

