package tests

import com.hhkungfu.HHKungfuParser
import com.hh3d.HH3DParser

private fun assertThat(value: Boolean, message: String) {
    if (!value) error(message)
}

fun main() {
    val kungfuCatalog = """
        <a href="/dai-chua-te-3d"><img src="/poster.jpg" alt="Đại Chúa Tể 3D"></a>
        <a href="/watch-dai-chua-te-3d/tap-90-sv1.html">Tập 90</a>
    """.trimIndent()
    val kCards = HHKungfuParser.parseCatalog(kungfuCatalog, "https://hhkungfu.ee")
    assertThat(kCards.size == 1, "HHKungfu catalog should keep only detail links")
    assertThat(kCards.first().title == "Đại Chúa Tể 3D", "HHKungfu title")
    assertThat(kCards.first().posterUrl == "https://hhkungfu.ee/poster.jpg", "HHKungfu poster")

    val kungfuDetail = """
        <meta property="og:image" content="https://img.example/k.jpg">
        <h1>Đại Chúa Tể 3D</h1><h2>The Great Ruler 3D</h2>
        <p>Thể Loại: <a href="/the-loai/tu-tien">Tu Tiên</a></p>
        <h3>Nội dung phim</h3><p>Một hành trình tu luyện.</p>
        <h3>Vietsub</h3>
        <a href="/watch-dai-chua-te-3d/tap-90-sv1.html">Tập 90</a>
        <h3>Thuyết Minh</h3>
        <a href="/watch-dai-chua-te-3d/tap-90-tm.html">Tập 90</a>
    """.trimIndent()
    val kd = HHKungfuParser.parseDetail(kungfuDetail, "https://hhkungfu.ee", "https://hhkungfu.ee/dai-chua-te-3d")
    assertThat(kd.title == "Đại Chúa Tể 3D", "HHKungfu detail title")
    assertThat(kd.altTitle == "The Great Ruler 3D", "HHKungfu alt title")
    assertThat(kd.episodes.count { !it.dubbed } == 1, "HHKungfu sub episode")
    assertThat(kd.episodes.count { it.dubbed } == 1, "HHKungfu dub episode")

    val hh3dCatalog = """
        <a href="/gia-thien"><img data-src="/gia-thien.jpg" alt="Già Thiên"></a>
        <a href="/gia-thien/vietsub/tap-180">180</a>
    """.trimIndent()
    val hCards = HH3DParser.parseCatalog(hh3dCatalog, "https://www.hh3d.com")
    assertThat(hCards.size == 1, "HH3D catalog should keep only detail links")
    assertThat(hCards.first().title == "Già Thiên", "HH3D title")

    val hh3dDetail = """
        <meta property="og:image" content="https://img.example/g.jpg">
        <h1>Già Thiên</h1><h2>Shrouding The Heavens</h2>
        <a href="/nam/2023">2023</a>
        <h3>Nội dung phim</h3><p>Diệp Phàm bước vào con đường tu luyện.</p>
        <a href="/gia-thien/vietsub/tap-180">180</a>
        <a href="/gia-thien/thuyet-minh/tap-180">180</a>
    """.trimIndent()
    val hd = HH3DParser.parseDetail(hh3dDetail, "https://www.hh3d.com", "https://www.hh3d.com/gia-thien")
    assertThat(hd.year == 2023, "HH3D year")
    assertThat(hd.episodes.count { !it.dubbed } == 1, "HH3D sub episode")
    assertThat(hd.episodes.count { it.dubbed } == 1, "HH3D dub episode")

    println("Parser tests passed: 10 assertions")
}
