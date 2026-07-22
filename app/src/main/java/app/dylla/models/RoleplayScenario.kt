package app.dylla.models

import java.util.UUID
import java.util.Date

enum class Difficulty(val label: String, val xpMultiplier: Int) {
    BEGINNER("Beginner", 1),
    INTERMEDIATE("Intermediate", 2),
    ADVANCED("Advanced", 3),
    EXPERT("Expert", 4)
}

data class RoleplayScenario(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val merchantName: String,
    val industry: String,
    val monthlyRevenue: String,
    val painPoint: String,
    val difficulty: Difficulty,
    val objections: List<String>,
    val personality: String,
    val timeInBusiness: String = "3 years"
)

data class RoleplayMessage(
    val id: String = UUID.randomUUID().toString(),
    val role: String,
    val content: String,
    val coachTip: CoachTip? = null
)

data class CoachTip(
    val message: String,
    val type: CoachTipType
)

enum class CoachTipType {
    GOOD_MOVE,
    MISSED_OPPORTUNITY
}

data class SessionScoreResult(
    val grade: String = "",
    val strengths: List<String> = emptyList(),
    val improvements: List<String> = emptyList(),
    val bestLine: String = "",
    val tip: String = "",
    val objectionsClosed: Int = 0,
    val xp: Int = 0
)

data class SessionRecord(
    val id: String = UUID.randomUUID().toString(),
    val scenarioTitle: String,
    val industry: String,
    val difficulty: Difficulty,
    val grade: String,
    val xpEarned: Int,
    val date: Date = Date()
)

data class TrainingStats(
    var totalXP: Int = 0,
    var totalSessions: Int = 0,
    var objectionsHandled: Int = 0,
    var currentStreak: Int = 0,
    var longestStreak: Int = 0,
    var sessionHistory: MutableList<SessionRecord> = mutableListOf()
) {
    val level: Int get() = (totalXP / 500) + 1

    val rank: String
        get() = when {
            level >= 20 -> "Closer Elite"
            level >= 15 -> "Senior Closer"
            level >= 10 -> "Closer"
            level >= 7 -> "Senior Rep"
            level >= 5 -> "Sales Rep"
            level >= 3 -> "Junior Rep"
            else -> "Trainee"
        }

    val xpProgress: Float
        get() {
            val xpInCurrentLevel = totalXP % 500
            return xpInCurrentLevel / 500f
        }

    val xpToNextLevel: Int
        get() = 500 - (totalXP % 500)
}

object ScenarioBank {
    val scenarios: List<RoleplayScenario> = listOf(
        // --- Beginner ---
        RoleplayScenario(
            title = "The Busy Owner",
            merchantName = "Mike Rodriguez",
            industry = "Restaurant",
            monthlyRevenue = "$45,000",
            painPoint = "Needs a new pizza oven but cash is tight after slow winter",
            difficulty = Difficulty.BEGINNER,
            objections = listOf(
                "I'm really busy right now, can you call back?",
                "How much does it cost?",
                "I need to think about it"
            ),
            personality = "Friendly but distracted. Running a busy lunch rush. Will engage if you're quick and respectful of their time."
        ),
        RoleplayScenario(
            title = "The Curious Starter",
            merchantName = "Sarah Chen",
            industry = "Retail",
            monthlyRevenue = "$30,000",
            painPoint = "Wants to expand inventory for holiday season but bank turned her down",
            difficulty = Difficulty.BEGINNER,
            objections = listOf(
                "What exactly is this?",
                "Is this a loan?",
                "How fast can I get the money?"
            ),
            personality = "Genuinely curious and open-minded. First time exploring alternative funding. Asks straightforward questions."
        ),
        RoleplayScenario(
            title = "The Referral Lead",
            merchantName = "James Wilson",
            industry = "Construction",
            monthlyRevenue = "$80,000",
            painPoint = "His buddy got funded and told him to call. Needs capital for new equipment.",
            difficulty = Difficulty.BEGINNER,
            objections = listOf(
                "My friend said he got a good deal, can I get the same?",
                "How long is the process?",
                "What documents do you need?"
            ),
            personality = "Warm and trusting because of the referral. Expects a smooth process. Easy to work with if you're professional."
        ),
        RoleplayScenario(
            title = "The Online Inquiry",
            merchantName = "Lisa Park",
            industry = "Salon",
            monthlyRevenue = "$25,000",
            painPoint = "Filled out an online form for business funding. Wants to renovate her salon.",
            difficulty = Difficulty.BEGINNER,
            objections = listOf(
                "I filled out a form online, I'm not sure which company this is",
                "How is this different from a bank loan?",
                "What are the payments like?"
            ),
            personality = "Polite but slightly cautious since she filled out forms with multiple companies. Will commit if you stand out."
        ),

        // --- Intermediate ---
        RoleplayScenario(
            title = "The Rate Shopper",
            merchantName = "David Kim",
            industry = "Medical",
            monthlyRevenue = "$120,000",
            painPoint = "Needs equipment financing but is shopping multiple lenders for the best rate",
            difficulty = Difficulty.INTERMEDIATE,
            objections = listOf(
                "What's your rate? Another company offered me 1.19",
                "Can you beat their offer?",
                "I'm going to wait and compare a few more offers",
                "Your rate seems higher than everyone else"
            ),
            personality = "Analytical and numbers-focused. Has done research. Will push hard on pricing but is open to value arguments beyond just rate."
        ),
        RoleplayScenario(
            title = "The Skeptic",
            merchantName = "Tom Bradley",
            industry = "Automotive",
            monthlyRevenue = "$65,000",
            painPoint = "Shop needs new diagnostic equipment but doesn't trust alternative lenders",
            difficulty = Difficulty.INTERMEDIATE,
            objections = listOf(
                "This sounds like a scam",
                "Why can't I just go to my bank?",
                "I've heard horror stories about merchant cash advances",
                "What's the catch?"
            ),
            personality = "Distrustful and blunt. Has heard bad things about the industry. Needs transparency and patience to build trust."
        ),
        RoleplayScenario(
            title = "The Procrastinator",
            merchantName = "Angela Torres",
            industry = "Retail",
            monthlyRevenue = "$40,000",
            painPoint = "Knows she needs capital for inventory but keeps putting it off",
            difficulty = Difficulty.INTERMEDIATE,
            objections = listOf(
                "Let me think about it and call you back next week",
                "I'm not ready to commit to anything today",
                "Can you just email me the info?",
                "Maybe after the holidays"
            ),
            personality = "Agreeable but non-committal. Will say yes to everything but never take action. Needs urgency and a compelling reason to move now."
        ),
        RoleplayScenario(
            title = "The Competitor Loyalist",
            merchantName = "Robert Hayes",
            industry = "Construction",
            monthlyRevenue = "$150,000",
            painPoint = "Has an existing funder he's loyal to but their renewal terms got worse",
            difficulty = Difficulty.INTERMEDIATE,
            objections = listOf(
                "I already have a company I work with",
                "They've always taken care of me",
                "Why should I switch?",
                "I don't want to start over with someone new"
            ),
            personality = "Loyal but frustrated with his current provider. Needs to feel that switching isn't risky and that you'll treat him better."
        ),

        // --- Advanced ---
        RoleplayScenario(
            title = "The Burned Borrower",
            merchantName = "Patricia Gomez",
            industry = "Restaurant",
            monthlyRevenue = "$55,000",
            painPoint = "Got burned by a predatory lender. Stacked advances nearly killed her business.",
            difficulty = Difficulty.ADVANCED,
            objections = listOf(
                "I swore I'd never do this again",
                "The last company lied about the terms",
                "My payments were double what they told me",
                "How do I know you won't do the same thing?",
                "I'm still paying off the last one"
            ),
            personality = "Hurt and angry from past experience. Has trauma around funding. Needs empathy, transparency, and proof that you're different."
        ),
        RoleplayScenario(
            title = "The Cash Flow Expert",
            merchantName = "William Chen",
            industry = "Tech",
            monthlyRevenue = "$200,000",
            painPoint = "SaaS company with strong MRR but delayed enterprise payments creating gaps",
            difficulty = Difficulty.ADVANCED,
            objections = listOf(
                "I understand my cash flow better than you do",
                "Your cost of capital seems high for our revenue level",
                "We're exploring a Series A, won't this create issues?",
                "What happens if we want to prepay?",
                "Can you structure this around our billing cycles?"
            ),
            personality = "Sophisticated and financially literate. CFO-level thinking. Expects you to match their level of knowledge and offer creative structuring."
        ),
        RoleplayScenario(
            title = "The Legal Eagle",
            merchantName = "Katherine Brooks",
            industry = "Legal",
            monthlyRevenue = "$95,000",
            painPoint = "Law firm needs capital for case expenses but wants every detail in writing",
            difficulty = Difficulty.ADVANCED,
            objections = listOf(
                "I need to review the full contract before we proceed",
                "What's the effective APR?",
                "Is this regulated in our state?",
                "I want my attorney to look at this",
                "What happens in case of default?"
            ),
            personality = "Meticulous and detail-oriented. Will scrutinize every word. Respects competence and thoroughness. Dislikes pressure tactics."
        ),
        RoleplayScenario(
            title = "The Multi-Location",
            merchantName = "Frank DiMaggio",
            industry = "Restaurant",
            monthlyRevenue = "$250,000",
            painPoint = "Runs 3 restaurants, wants to open a 4th, needs significant capital",
            difficulty = Difficulty.ADVANCED,
            objections = listOf(
                "I need at least $300K, can you do that?",
                "I want one payment across all locations",
                "My accountant handles all financial decisions",
                "What's the largest deal you've done?",
                "I've been in business 15 years, I know what I'm doing"
            ),
            personality = "Experienced and commands respect. Expects white-glove service. Has leverage because of his size and will use it."
        ),

        // --- Expert ---
        RoleplayScenario(
            title = "The Hostile Gatekeeper",
            merchantName = "Diane Marshall",
            industry = "Medical",
            monthlyRevenue = "$175,000",
            painPoint = "Office manager screening calls. Doctor needs capital for new imaging equipment.",
            difficulty = Difficulty.EXPERT,
            objections = listOf(
                "Dr. Marshall isn't available and doesn't take sales calls",
                "We're not interested in any funding",
                "He told me to tell everyone to stop calling",
                "If I put every salesperson through he'd never see patients",
                "Send me something in writing and maybe he'll look at it",
                "You're the third person today calling about this"
            ),
            personality = "Protective and dismissive. Her job is to keep salespeople away. Only lets through people who demonstrate real value or insider knowledge."
        ),
        RoleplayScenario(
            title = "The Savvy Negotiator",
            merchantName = "Marcus Johnson",
            industry = "Construction",
            monthlyRevenue = "$300,000",
            painPoint = "Large contractor who knows the industry. Plays funders against each other.",
            difficulty = Difficulty.EXPERT,
            objections = listOf(
                "I've got 3 offers on my desk right now",
                "Company X offered me a 1.15 factor with daily payments",
                "I want a 1.10 or I'm going with them",
                "Can you throw in the first month free?",
                "My current funder will match whatever you offer",
                "I close deals for a living, so don't try to close me"
            ),
            personality = "Alpha personality. Master negotiator. Respects strength and despises weakness. Will walk if he smells desperation."
        ),
        RoleplayScenario(
            title = "The Board Decision",
            merchantName = "Elizabeth Warren-Price",
            industry = "Tech",
            monthlyRevenue = "$500,000",
            painPoint = "VP of Finance at a mid-size company. Any capital decision needs board approval.",
            difficulty = Difficulty.EXPERT,
            objections = listOf(
                "I can't make this decision alone, the board has to approve",
                "We'd need a detailed proposal with projections",
                "How does this impact our balance sheet?",
                "Our CFO would need to be on the call",
                "The board meets quarterly, so this would take 3 months",
                "We typically only work with top-tier financial institutions"
            ),
            personality = "Corporate and process-driven. Genuinely interested but bound by bureaucracy. Needs help building the internal business case."
        ),
        RoleplayScenario(
            title = "The Industry Veteran",
            merchantName = "Carl Napolitano",
            industry = "Construction",
            monthlyRevenue = "$400,000",
            painPoint = "40 years in business, semi-retired, son runs daily ops. Needs capital for a big government contract.",
            difficulty = Difficulty.EXPERT,
            objections = listOf(
                "Son, I've been in this business longer than you've been alive",
                "I built this company without borrowing a dime",
                "My bank gives me whatever I need, they just take too long",
                "I don't do business on the phone",
                "You need to come see the operation in person",
                "What's your company's track record? How long have you been around?"
            ),
            personality = "Old school, commanding, expects deference. Values relationships over transactions. Won't be rushed or pressured. Respects honesty and directness."
        )
    )

    fun forDifficulty(difficulty: Difficulty): List<RoleplayScenario> {
        return scenarios.filter { it.difficulty == difficulty }
    }

    fun byId(id: String): RoleplayScenario? {
        return scenarios.firstOrNull { it.id == id }
    }
}
