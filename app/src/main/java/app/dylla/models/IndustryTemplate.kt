package app.dylla.models

data class IndustryTemplate(
    val industry: IndustryType,
    val smsTemplates: List<String>,
    val emailTemplates: List<String>,
    val objectionHandlers: Map<String, String>
) {
    companion object {
        val restaurant = IndustryTemplate(
            industry = IndustryType.RESTAURANT,
            smsTemplates = listOf(
                "Hi {name}, I work with restaurant owners in your area who needed quick capital for equipment upgrades or renovations. Would you be open to a quick chat?",
                "Hey {name}, I noticed {business} and wanted to reach out. We help restaurants access working capital in as little as 24 hours. Interested?",
                "Hi {name}, many restaurant owners use our flexible funding to cover seasonal gaps or expand. Would that be helpful for {business}?"
            ),
            emailTemplates = listOf(
                "Subject: Capital options for {business}\n\nHi {name},\n\nI specialize in helping restaurant owners like you access working capital quickly -- whether it's for new equipment, renovations, or bridging a slow season.\n\nWe offer flexible terms with funding in as little as 24 hours.\n\nWould you have 5 minutes this week for a quick call?\n\nBest regards",
                "Subject: Quick funding for restaurants like {business}\n\nHi {name},\n\nI've helped dozens of restaurant owners in your area get the capital they need to grow. No lengthy applications, no collateral required.\n\nWould you be open to learning more?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "I understand margins are tight in the restaurant business. Our programs are designed around your daily revenue, so payments flex with your cash flow.",
                "bad credit" to "We work with many restaurant owners who've had credit challenges. We focus on your daily deposits, not your credit score.",
                "not interested" to "No problem at all. If things change or you need capital for equipment, expansion, or a slow season, keep my number handy.",
                "already have funding" to "That's great -- many of our restaurant clients use a second position to fund growth without touching their first. Worth a quick look?"
            )
        )

        val retail = IndustryTemplate(
            industry = IndustryType.RETAIL,
            smsTemplates = listOf(
                "Hi {name}, I help retail businesses access fast working capital for inventory, expansion, or cash flow. Would {business} benefit from that?",
                "Hey {name}, stocking up for the season? We help retail owners like you get funded in 24 hours. Want to learn more?",
                "Hi {name}, if {business} could use extra capital for inventory or growth, I'd love to show you what's available."
            ),
            emailTemplates = listOf(
                "Subject: Working capital for {business}\n\nHi {name},\n\nRetail moves fast, and so should your funding. We help retail business owners access working capital in as little as 24 hours -- perfect for inventory purchases, expansion, or bridging cash flow gaps.\n\nWould you be open to a quick conversation?\n\nBest regards",
                "Subject: Grow {business} with flexible funding\n\nHi {name},\n\nI work with retail owners who need fast access to capital without the hassle of traditional bank loans.\n\nLet me know if you'd like to explore your options.\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "I hear you. Our retail clients find that the revenue generated from stocking up or expanding more than covers the cost of funding.",
                "bad credit" to "Credit score isn't the main factor for us. We look at your daily sales volume, which is great news for most retail businesses.",
                "not interested" to "Understood. If you ever need capital for inventory or expansion, feel free to reach out anytime.",
                "already have funding" to "Perfect -- a lot of our retail clients layer funding to maximize inventory during peak seasons. Happy to show you the math."
            )
        )

        val medical = IndustryTemplate(
            industry = IndustryType.MEDICAL,
            smsTemplates = listOf(
                "Hi {name}, I work with medical practices that need capital for equipment, expansion, or cash flow. Would {business} benefit from flexible funding?",
                "Hey {name}, we help medical professionals access working capital quickly. No lengthy underwriting -- funded in as little as 24 hours. Interested?",
                "Hi {name}, many practices use our funding for new equipment or hiring. Would that be useful for {business}?"
            ),
            emailTemplates = listOf(
                "Subject: Capital solutions for {business}\n\nHi {name},\n\nI specialize in funding for medical practices. Whether you need capital for new equipment, additional staff, or practice expansion, we offer fast and flexible options.\n\nWould you have a few minutes to discuss?\n\nBest regards",
                "Subject: Flexible funding for your practice\n\nHi {name},\n\nMany medical professionals face cash flow gaps between billing and collections. Our programs are designed to bridge those gaps and help you invest in growth.\n\nLet me know if you'd like to explore options.\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "I understand cost is a concern. Many of our medical clients find that investing in new equipment or staff generates revenue that far exceeds the cost of capital.",
                "bad credit" to "We focus on your practice's revenue, not personal credit. If your practice is generating consistent income, you likely qualify.",
                "not interested" to "No worries. If your practice ever needs capital for equipment, expansion, or cash flow, I'm here to help.",
                "already have funding" to "That's smart. Many practices use additional funding to invest in growth while keeping their existing capital working."
            )
        )

        val legal = IndustryTemplate(
            industry = IndustryType.LEGAL,
            smsTemplates = listOf(
                "Hi {name}, I help law firms access working capital for case costs, marketing, or expansion. Would {business} benefit from flexible funding?",
                "Hey {name}, we fund law firms in as little as 24 hours. No collateral needed. Want to learn more?",
                "Hi {name}, if {business} could use capital for growth or bridging case settlements, I'd love to connect."
            ),
            emailTemplates = listOf(
                "Subject: Capital solutions for {business}\n\nHi {name},\n\nI work with law firms that need flexible capital for case costs, marketing, hiring, or expansion. Our programs are designed for professional services -- fast approval, no collateral.\n\nWould you be open to a brief call?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "Understood. Our legal clients typically use funding to take on more cases, which generates multiples on the investment.",
                "bad credit" to "We underwrite based on your firm's revenue, not personal credit scores. Most established firms qualify.",
                "not interested" to "No problem. If your firm ever needs capital for case costs or growth, keep my info handy.",
                "already have funding" to "Great. Many firms use additional capital strategically for marketing or taking on bigger cases."
            )
        )

        val construction = IndustryTemplate(
            industry = IndustryType.CONSTRUCTION,
            smsTemplates = listOf(
                "Hi {name}, I help construction companies access fast capital for materials, equipment, or payroll. Would {business} benefit?",
                "Hey {name}, we fund construction businesses in 24 hours. Perfect for bridging between jobs. Interested?",
                "Hi {name}, if {business} needs capital for a new project or equipment, I'd love to show you options."
            ),
            emailTemplates = listOf(
                "Subject: Working capital for {business}\n\nHi {name},\n\nConstruction moves fast, and cash flow gaps between jobs can slow you down. We help contractors and construction companies access capital in as little as 24 hours.\n\nWould you be open to learning more?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "I get it. Our construction clients use funding to take on bigger projects that generate significantly more revenue.",
                "bad credit" to "We focus on your business revenue and deposits, not credit scores. If you're landing jobs, you likely qualify.",
                "not interested" to "No worries. If you ever need capital between projects, don't hesitate to reach out.",
                "already have funding" to "Smart. Many contractors stack capital to handle multiple projects simultaneously."
            )
        )

        val realEstate = IndustryTemplate(
            industry = IndustryType.REAL_ESTATE,
            smsTemplates = listOf(
                "Hi {name}, I help real estate professionals access fast capital for deals, marketing, or operations. Would {business} benefit?",
                "Hey {name}, we fund RE businesses in 24 hours. No collateral beyond revenue. Want to learn more?",
                "Hi {name}, if {business} needs working capital for growth or bridging closings, I'd love to connect."
            ),
            emailTemplates = listOf(
                "Subject: Capital solutions for {business}\n\nHi {name},\n\nI work with real estate professionals who need flexible working capital. Whether it's marketing, bridging closings, or scaling operations, we offer fast funding with no real estate collateral required.\n\nWould you have a few minutes to chat?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "Many of our RE clients find that fast capital helps them close deals they'd otherwise miss, generating returns that far exceed the cost.",
                "bad credit" to "We base approval on your business revenue, not personal credit. Active real estate businesses typically qualify.",
                "not interested" to "Understood. If you ever need capital to close a deal or scale marketing, I'm a call away.",
                "already have funding" to "Great. Additional working capital can help you pursue more deals without tying up existing lines."
            )
        )

        val automotive = IndustryTemplate(
            industry = IndustryType.AUTOMOTIVE,
            smsTemplates = listOf(
                "Hi {name}, I help auto shops and dealers access fast capital for inventory, equipment, or expansion. Would {business} benefit?",
                "Hey {name}, we fund automotive businesses in 24 hours. Need capital for parts or equipment? Let's talk.",
                "Hi {name}, if {business} needs working capital, I can show you options that work with your cash flow."
            ),
            emailTemplates = listOf(
                "Subject: Capital for {business}\n\nHi {name},\n\nI help automotive businesses access working capital for inventory, equipment upgrades, or expansion. Fast approval, flexible terms.\n\nInterested in learning more?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "Our automotive clients find that investing in inventory or equipment generates enough additional revenue to easily cover the cost.",
                "bad credit" to "We focus on your shop's daily revenue, not credit scores. If you're doing consistent business, you likely qualify.",
                "not interested" to "No problem. Keep my number in case you ever need capital for parts, equipment, or expansion.",
                "already have funding" to "Smart move. Many shops use additional capital to stock up on parts or add service bays."
            )
        )

        val salon = IndustryTemplate(
            industry = IndustryType.SALON,
            smsTemplates = listOf(
                "Hi {name}, I help salon owners access fast capital for renovations, equipment, or marketing. Would {business} benefit?",
                "Hey {name}, we fund beauty and salon businesses in 24 hours. Interested in learning more?",
                "Hi {name}, if {business} could use capital for upgrades or expansion, I'd love to show you what's available."
            ),
            emailTemplates = listOf(
                "Subject: Grow {business} with flexible funding\n\nHi {name},\n\nI work with salon and beauty business owners who want to renovate, expand, or invest in marketing. Our funding is fast and works with your cash flow.\n\nWould you like to explore your options?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "I understand. Many salon owners find that a renovation or marketing push brings in enough new clients to cover the investment quickly.",
                "bad credit" to "We look at your salon's daily revenue, not credit history. Consistent income is what matters most.",
                "not interested" to "No worries. If you ever want to renovate or expand, feel free to reach out.",
                "already have funding" to "Great. Additional capital can help you invest in marketing or add stations without touching existing funds."
            )
        )

        val fitness = IndustryTemplate(
            industry = IndustryType.FITNESS,
            smsTemplates = listOf(
                "Hi {name}, I help gym and fitness studio owners access capital for equipment, expansion, or marketing. Would {business} benefit?",
                "Hey {name}, we fund fitness businesses in 24 hours. Need new equipment or want to expand? Let's talk.",
                "Hi {name}, if {business} could use capital for growth, I have flexible options to share."
            ),
            emailTemplates = listOf(
                "Subject: Capital for {business}\n\nHi {name},\n\nI specialize in funding for fitness businesses. Whether you need new equipment, want to expand, or need help with cash flow during slow months, we can help.\n\nInterested in a quick call?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "New equipment and expanded space directly drive membership growth. Most gym owners see a strong ROI from capital investments.",
                "bad credit" to "We base decisions on your gym's revenue, not personal credit. If you have consistent membership income, you're likely a fit.",
                "not interested" to "Understood. If you ever need capital for equipment or expansion, don't hesitate to reach out.",
                "already have funding" to "Smart. Additional funding can help you add equipment or locations while keeping your current capital working."
            )
        )

        val tech = IndustryTemplate(
            industry = IndustryType.TECH,
            smsTemplates = listOf(
                "Hi {name}, I help tech companies access working capital for hiring, marketing, or scaling. Would {business} benefit?",
                "Hey {name}, we fund tech businesses in 24 hours. Need capital to scale or hire? Let's connect.",
                "Hi {name}, if {business} needs capital for growth, I have flexible programs designed for tech companies."
            ),
            emailTemplates = listOf(
                "Subject: Growth capital for {business}\n\nHi {name},\n\nI work with tech companies that need flexible working capital for hiring, marketing, or scaling operations. Fast approval, no equity dilution.\n\nWould you be open to exploring options?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "Unlike equity financing, our capital doesn't dilute your ownership. Many tech founders prefer revenue-based financing for that reason.",
                "bad credit" to "We focus on business revenue and contracts, not personal credit. SaaS and recurring revenue businesses are a great fit.",
                "not interested" to "No problem. If you ever need non-dilutive capital for hiring or growth, keep my info handy.",
                "already have funding" to "Great. Non-dilutive working capital pairs well with equity funding -- lets you extend runway without giving up more ownership."
            )
        )

        val other = IndustryTemplate(
            industry = IndustryType.OTHER,
            smsTemplates = listOf(
                "Hi {name}, I help business owners like you access fast working capital. Would {business} benefit from flexible funding?",
                "Hey {name}, we offer funding in as little as 24 hours. No lengthy applications. Want to learn more?",
                "Hi {name}, if {business} could use capital for growth, operations, or cash flow, I'd love to show you what's available."
            ),
            emailTemplates = listOf(
                "Subject: Working capital for {business}\n\nHi {name},\n\nI help businesses access fast, flexible working capital. Whether you need funding for growth, equipment, inventory, or cash flow, we have programs designed for your needs.\n\nWould you be open to a quick conversation?\n\nBest regards"
            ),
            objectionHandlers = mapOf(
                "too expensive" to "I understand cost is a concern. Our programs are designed to work with your cash flow, and most clients find the investment pays for itself.",
                "bad credit" to "We focus on your business revenue, not credit scores. If your business is generating consistent income, you likely qualify.",
                "not interested" to "No worries at all. If things change, feel free to reach out anytime.",
                "already have funding" to "That's great. Additional working capital can help you invest in growth without touching your existing lines."
            )
        )

        val all: List<IndustryTemplate> = listOf(
            restaurant, retail, medical, legal, construction,
            realEstate, automotive, salon, fitness, tech, other
        )

        fun forIndustry(industry: IndustryType): IndustryTemplate {
            return all.firstOrNull { it.industry == industry } ?: other
        }
    }
}
