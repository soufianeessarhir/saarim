package com.sarim.app.data

/**
 * The content that is blocked from the first launch, in every language, and that nothing in the
 * app can remove.
 *
 * The user's own additions live next to these — a browser list — but the entries here are
 * re-applied on every launch by [com.sarim.app.security.MandatoryProtection], so deleting them
 * from the UI only makes them come back on the next start (the screens also refuse to save a
 * list with them missing). The built-in lists themselves are not shown in the UI: there is
 * nothing useful for the user to do with a list they cannot remove.
 */
object ProtectionDefaults {

    /** Device-owner private DNS: filters adult domains before a browser ever resolves them. */
    const val FILTERING_DNS_HOST = "family.cloudflare-dns.com"

    /**
     * Hosts the private-DNS picker offers. Every one of them is a family/adult-filtering
     * resolver; there is deliberately no "automatic" or "off", because a resolver of the user's
     * choosing is the shortest route around URL blocking, and a VPN is blocked separately by
     * [com.sarim.app.security.MandatoryProtection.RESTRICTIONS].
     */
    val FILTERING_DNS_HOSTS: List<String> = listOf(
        "family.cloudflare-dns.com" to "Cloudflare Family",
        "family.adguard-dns.com" to "AdGuard Family",
        "adult-filter-dns.cleanbrowsing.org" to "CleanBrowsing Adult"
    ).let { it.map { (host, _) -> host } }

    val FILTERING_DNS_LABELS: Map<String, String> = mapOf(
        "family.cloudflare-dns.com" to "Cloudflare Family",
        "family.adguard-dns.com" to "AdGuard Family",
        "adult-filter-dns.cleanbrowsing.org" to "CleanBrowsing Adult"
    )

    /**
     * The always-present `URLBlocklist` list pushed into every browser that supports managed
     * restrictions. Kept as bare hosts: `URLBlocklist` matches the host and its subdomains, so
     * `pornhub.com` covers `www.pornhub.com` and `m.pornhub.com` without extra entries.
     */
    const val MANDATORY_BROWSER_LIST_ID = "sarim-mandatory-adult-sites"

    val ADULT_SITES: List<String> = listOf(
        // Major tube and streaming sites.
        "pornhub.com", "xvideos.com", "xnxx.com", "xhamster.com", "redtube.com",
        "youporn.com", "spankbang.com", "tube8.com", "porntrex.com", "txxx.com",
        "eporner.com", "beeg.com", "porn.com", "sex.com", "pornmd.com",
        "thumbzilla.com", "porn300.com", "pornone.com", "hqporner.com", "yespornplease.com",
        "drtuber.com", "sexvid.xxx", "pornhat.com", "porn00.org", "hotmovs.com",
        "tubegalore.com", "pornktube.com", "porn555.com", "pornid.com", "pornlib.com",
        "porn7.net", "pornhd.com", "pornhd3x.com", "porn4k.com", "pornoxo.com",
        "pornmz.com", "pornvid.com", "porndig.com", "porngo.com", "pornburst.com",
        "pornwhite.com", "porntube.com", "porntop.com", "porndoe.com", "porndish.com",
        "pornflip.com", "pornicom.com", "porny.com", "pornzog.com", "pornobae.com",
        "pornone.tv", "porneq.com", "pornhd4k.com", "pornhublive.com", "pornobaba.com",
        "pornodrome.com", "pornofilms.com", "pornomovies.com", "pornorama.com", "pornos.com",
        "pornotv.com", "pornovideos.com", "pornslash.com", "pornstarbox.com", "pornstream.com",
        "pornteen.com", "pornultra.com", "pornvidz.com", "pornwild.com", "pornxxx.com",
        "porno365.net", "porno666.com", "pornocrush.com", "pornomix.com", "pornoonline.com",
        "pornotube.com", "pornozone.com", "pornportal.com", "pornshark.com", "pornstartube.com",
        "pornstreams.com", "porntubes.com", "pornvideo.com", "pornvideos.com", "pornvideoz.com",
        "pornvids.com", "pornxp.com", "pornzee.com", "porzo.com", "pornworld.com",
        "pornon.com", "pornover.com", "pornstarsex.com", "porntubehd.com", "pornvisit.com",
        "pornzoom.com", "pornamateurs.com", "pornbrazil.com", "porncams.com", "pornclassics.com",
        "pornclip.com", "porndad.com", "pornfree.com", "pornfriends.com", "porngames.com",
        "porngay.com", "porgasm.com", "porhub.com", "pornhot.com", "pornhq.com",
        "pornhut.com", "pornia.com", "pornleak.com", "pornlinks.com", "pornlive.com",
        "pornload.com", "pornmovies.com", "pornnetwork.com", "pornnow.com", "pornoblog.com",
        "pornochannel.com", "pornoclip.com", "pornoclub.com", "pornodigital.com", "pornodrive.com",
        "pornofactory.com", "pornofan.com", "pornofilm.com", "pornofix.com", "pornoflix.com",
        "pornogirl.com", "pornogold.com", "pornogratis.com", "pornohaus.com", "pornohaven.com",
        "pornohome.com", "pornohunter.com", "pornoizle.com", "pornoking.com", "pornolab.com",
        "pornoland.com", "pornolife.com", "pornolist.com", "pornolive.com", "pornoloco.com",
        "pornolove.com", "pornomag.com", "pornomania.com", "pornomedia.com", "pornomilf.com",
        "pornomodel.com", "pornomom.com", "pornonight.com", "pornonline.com", "pornoparadise.com",
        "pornoparty.com", "pornopassion.com", "pornopics.com", "pornoplace.com", "pornoplanet.com",
        "pornopremium.com", "pornoprime.com", "pornorama.tv", "pornored.com", "pornoroom.com",
        "pornosearch.com", "pornoshop.com", "pornoshow.com", "pornosite.com", "pornosky.com",
        "pornosource.com", "pornospace.com", "pornospot.com", "pornostar.com", "pornostars.com",
        "pornostore.com", "pornostream.com", "pornostreet.com", "pornostudio.com", "pornosun.com",
        "pornotape.com", "pornotaxi.com", "pornotime.com", "pornotoon.com", "pornotop.com",
        "pornotown.com", "pornotube.net", "pornotubehd.com", "pornovideo.com", "pornovideohd.com",
        "pornovids.com", "pornoville.com", "pornovision.com", "pornovista.com", "pornoweb.com",
        "pornoworld.com", "pornox.com", "pornoxd.com", "pornoxo.net", "pornoxxx.com",
        "pornozavr.com", "4tube.com", "3movs.com", "vporn.com", "vjav.com",
        "vxxx.com", "veporn.com", "vipsexvids.com", "vrporn.com", "anysex.com",
        "anyporn.com", "hellporno.com", "hdzog.com", "hclips.com", "hdsex.com",
        "hdsex18.com", "javhd.com", "javhub.net", "jav.guru", "javdoe.com",
        "javmix.tv", "javwhores.com", "javporn.cc", "javsex.net", "javsin.com",
        "sunporno.com", "sexu.com", "sex3.com", "sexdino.com", "sexhd.com",
        "sexmovies.com", "sexvideos.com", "sexcelebrity.net", "sexkino.com", "sexyporn.com",
        "sexemara.com", "sexmex.com", "sexloading.com", "wankoz.com", "wetpussy.com",
        "wetplace.com", "xogogo.com", "xcafe.com", "9taxi.com", "ashemaletube.com",
        "shemalez.com", "tgirls.com", "tgirlstube.com", "boyfriendtv.com", "gayporn.com",
        "gaytube.com", "gaymaletube.com", "gayforit.eu", "gay0day.com", "men.com",
        "gayhoopla.com", "gaywire.com", "transangels.com", "tsplayground.com", "tstube.com",

        // Studios, networks and premium brands.
        "brazzers.com", "bangbros.com", "realitykings.com", "mofos.com", "naughtyamerica.com",
        "digitalplayground.com", "evilangel.com", "private.com", "21sextury.com", "dorcelclub.com",
        "sexart.com", "blacked.com", "vixen.com", "tushy.com", "deeper.com",
        "julesjordan.com", "milehighmedia.com", "sweetheartvideo.com", "girlsway.com", "allherluv.com",
        "twistys.com", "playboy.com", "playboyplus.com", "penthouse.com", "hustler.com",
        "x-art.com", "xart.com", "pornpros.com", "teamskeet.com", "nubilefilms.com",
        "nubiles.net", "babes.com", "babesnetwork.com", "dogfartnetwork.com", "dogfart.com",
        "kink.com", "kinkmen.com", "kinkuniversity.com", "publicagent.com", "fakehub.com",
        "fakeagent.com", "fakehospital.com", "faketaxi.com", "fakecop.com", "fakecasting.com",
        "propertysex.com", "pornfidelity.com", "passion-hd.com", "passionhd.com", "wowgirls.com",
        "lustcinema.com", "supernaturalxxx.com", "slayed.com", "allblackx.com", "darkx.com",
        "lesbiantube.com", "adulttime.com", "letsdoeit.com", "bang.com", "adultdvdempire.com",

        // Creator platforms and paid fan sites.
        "onlyfans.com", "fansly.com", "manyvids.com", "clips4sale.com", "justfor.fans",
        "fancentro.com", "avnstars.com", "loyalfans.com", "pocketstars.com", "ismygirl.com",
        "modelhub.com", "fansone.co", "fanvue.com",

        // Live cams and adult chat.
        "chaturbate.com", "stripchat.com", "cam4.com", "bongacams.com", "livejasmin.com",
        "myfreecams.com", "camsoda.com", "camwhores.tv", "camwhores.com", "camvideos.tv",
        "cams.com", "camster.com", "camdolls.com", "xlovecam.com", "imlive.com",
        "streamate.com", "flirt4free.com", "jerkmate.com", "sexcam.com", "sexchat.com",
        "xcams.com", "livecams.com", "camming.com", "camhub.com", "camonster.com",
        "camplace.com", "camrabbit.com", "camshow.com",

        // Hentai, manga and adult comics.
        "hentaihaven.org", "hanime.tv", "nhentai.net", "nhentai.com", "e-hentai.org",
        "exhentai.org", "hentai2read.com", "hitomi.la", "rule34.xxx", "rule34.paheal.net",
        "rule34.us", "rule34.art", "rule34video.com", "rule34.xyz", "gelbooru.com",
        "danbooru.donmai.us", "sankakucomplex.com", "fakku.net", "fakku.com", "tsumino.com",
        "e621.net", "yande.re", "konachan.com", "hentai.tv", "hentaihaven.com",
        "hentaifox.com", "hentaifox.tv", "hentaigasm.com", "hentai2play.com", "hentai20.com",
        "hentaicity.com", "hentai-foundry.com", "hentai-foundry.net", "hentaihere.com", "hentaimama.com",
        "hentaimangaz.com", "hentaiporn.com", "hentaisexy.com", "hentaistream.com", "hentaitube.com",
        "hentaiworld.com", "hentai-moon.com", "hentairox.com", "hentaisharing.com", "hentaiz.net",
        "hentaiz.com", "hentaifreak.com", "hentaihub.com", "hentaime.com", "hentaiplus.com",
        "hentairoom.com", "hentaixin.com", "hentaipanda.com", "hentai-kingdom.com", "hentaigallery.com",
        "hentaimangas.com", "multporn.net", "8muses.com", "8muses.xxx", "jabcomix.com",
        "milftoon.com", "milftoon.xxx", "shadbase.com", "savitabhabhi.com", "velamma.com",
        "dofantasy.com", "adultcomics.com", "sexcomics.com", "comicporn.com", "porncomics.me",
        "3dsexcomics.com",

        // Dating, escort and hookup sites.
        "adultfriendfinder.com", "ashleymadison.com", "seeking.com", "seekingarrangement.com", "escortdirectory.com",
        "eros.com", "skipthegames.com", "escortads.com", "adultsearch.com", "adultlook.com",
        "bedpage.com", "cityxguide.com", "listcrawler.com", "tryst.link", "slixa.com",
        "privatedelights.com", "eroticmonkey.com", "escortfish.com", "escortsaffair.com", "xescorts.com",
        "eurogirlsescort.com", "ts4rent.eu", "tsescorts.com", "eroticreview.com", "massageexchange.com",
        "rubmaps.com", "rubrankings.com", "fetlife.com", "alt.com", "adult.com",
        "xmatch.com", "adultmatchmaker.com", "benaughty.com", "sls.com", "swinglifestyle.com",
        "kasidie.com", "sdc.com",

        // Erotic fiction, forums and galleries.
        "literotica.com", "nifty.org", "asstr.org", "lushstories.com", "sexstories.com",
        "sextopics.com", "storiesonline.net", "eroticstories.com", "mysexstories.com", "hqsexstories.com",
        "adultdvdtalk.com", "motherless.com", "efukt.com", "redgifs.com", "erome.com",
        "bunkr.si",

        // Uncensored AI companions and generated adult content.
        "pornpen.ai", "unfaithful.ai", "candy.ai", "soulgen.ai", "pornx.ai",
        "seduced.ai", "crushon.ai", "joyland.ai", "spicychat.ai", "janitorai.com",
        "muah.ai", "fantasygf.ai",

        // Shock and gore sites historically bundled with adult blocklists.
        "bestgore.com", "theync.com", "8kun.top"
    )

    /**
     * Words the accessibility service refuses to let across the keyboard, or — once a per-app
     * passive list has been added — on screen. Written in lowercase; matching is
     * case-insensitive and word-boundary based.
     *
     * The list is not shown in the app: there is nothing the user can remove from it, and a
     * visible list is just a reminder of the vocabulary. The English entries skew to unambiguous
     * adult vocabulary (a broad list with the word "sex" on its own would block health and news
     * content the user still needs), while the adult *sites* are covered by DNS and URL blocking.
     */
    val FORBIDDEN_WORDS: Set<String> = setOf(
        // English
        "porn", "porno", "pornography", "pornographic", "pornstar", "pornstars", "xxx", "xxnx",
        "nsfw", "hentai", "hentaiz", "rule34", "nude", "nudes", "nudity", "naked",
        "cumshot", "cumshots", "blowjob", "blowjobs", "handjob", "handjobs", "anal sex",
        "creampie", "gangbang", "threesome", "orgy", "orgies", "milf", "dildo", "dildos",
        "vibrator", "vibrators", "masturbation", "masturbate", "masturbating", "orgasm",
        "orgasms", "camgirl", "camgirls", "camsex", "sexcam", "sexting", "incest", "lolicon",
        "shotacon", "bukakke", "bukkake", "deepthroat", "facial", "fisting", "fetish", "bdsm",
        "escort", "escorts", "brothel", "stripper", "strippers", "striptease", "onlyfans",
        "fansly", "nympho", "squirting", "shemale", "shemales", "transsexual porn", "bareback",
        "gloryhole", "upskirt", "chaturbate", "stripchat", "erotica", "erotic", "horny", "slut",
        "sluts", "whore", "whores", "hooker", "hookers", "prostitute", "prostitutes",
        "prostitution", "ahegao", "futanari", "doujinshi", "ecchi", "oppai", "redtube",
        "youporn", "xvideos", "xhamster", "pornhub", "spankbang", "brazzers",
        // Arabic
        "إباحي", "إباحية", "اباحي", "اباحية", "عاري", "عارية", "عري", "سكس", "جنس صريح",
        "شهواني", "شهوانية", "شهوة", "مومس", "دعارة", "بغاء", "بورنو", "هنتاي", "عاهرة",
        "عاهرات", "شرموطة", "قحبة", "زانية", "زواني", "مجون", "خلاعة", "أفلام إباحية",
        "صور عارية", "نيك", "سحاق", "لواط", "استمناء",
        // Portuguese
        "pornô", "pornografia", "pornográfico", "pornográficos", "pornôs", "putaria", "nudes",
        "pelada", "pelado", "peladas", "sexo explícito", "sexo anal", "sexo oral", "prostituição",
        "prostituta", "prostitutas", "hentai", "punheta", "orgia", "suruba", "xoxota", "pau duro",
        "boquete", "masturbação", "masturbar", "ejaculação", "gozo", "sacanagem", "safadeza",
        "vagabunda", "vagabundas", "buceta", "pênis", "vagina", "peitos", "nudez", "tarado",
        "tarada", "tesão", "erótico", "erótica"
    )

    /** The browser list object that [MANDATORY_BROWSER_LIST_ID] always resolves to. */
    fun mandatoryBrowserListTitle(): String = "Sarim — adult sites (always on)"
}
