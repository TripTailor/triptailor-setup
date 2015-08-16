package co.triptailor.setup

import com.typesafe.config.Config

trait AnnotatorService {
  def config: Config
}