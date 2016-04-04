package co.triptailor.setup.db.drivers

import com.github.tminglei.slickpg.{PgPlayJsonSupport, ExPostgresDriver, PgArraySupport}

trait ExtendedPostgresDriver extends ExPostgresDriver with PgArraySupport with PgPlayJsonSupport {

  def pgjson = "jsonb" // jsonb support is in postgres 9.4.0 onward; for 9.3.x use "json"

  override val api = ExtendedAPI

  object ExtendedAPI extends API with ArrayImplicits with JsonImplicits with PlayJsonPlainImplicits
}

object ExtendedPostgresDriver extends ExtendedPostgresDriver