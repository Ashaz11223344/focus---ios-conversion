package com.example.motivation.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.motivation.data.SettingsDataStore
import com.example.motivation.model.Achievement
import com.example.motivation.model.AchievementsList
import com.example.motivation.model.Affirmation
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.example.motivation.worker.StreakReminderWorker
import com.example.motivation.helper.NotificationHelper
import kotlinx.coroutines.delay

// --- UI State Models ---
enum class AffirmationState { PENDING, CORRECT, INCORRECT }

data class StreakUiState(
    val requiredAffirmation: Affirmation = Affirmation(""),
    val userTypedAffirmation: String = "",
    val isAffirmationCompletedToday: Boolean = false,
    val streakCount: Int = 0,
    val achievements: List<Achievement> = emptyList(),
    val affirmationState: AffirmationState = AffirmationState.PENDING
)

class StreakViewModel(application: Application) : AndroidViewModel(application) {

    private val settingsDataStore = SettingsDataStore(application)
    private val workManager = WorkManager.getInstance(application)
    private val notificationHelper = NotificationHelper(application)

    private val _uiState = MutableStateFlow(StreakUiState())
    val uiState: StateFlow<StreakUiState> = _uiState.asStateFlow()

    private val affirmations = listOf(
        Affirmation("I will do my work"),
        Affirmation("I am enough"),
        Affirmation("I will be better"),
        Affirmation("I show up for myself"),
        Affirmation("Consistency builds discipline"),
        Affirmation("I attract positive energy daily"),
        Affirmation("My potential is truly limitless"),
        Affirmation("I choose courage over comfort"),
        Affirmation("Challenges help me grow stronger"),
        Affirmation("I am capable and confident"),
        Affirmation("Today I choose joyful productivity"),
        Affirmation("My focus creates my reality"),
        Affirmation("I am the architect here"),
        Affirmation("Progress happens step by step"),
        Affirmation("I embrace my personal power"),
        Affirmation("My mind is clear today"),
        Affirmation("I turn obstacles into opportunities"),
        Affirmation("Success flows naturally to me"),
        Affirmation("I trust my journey completely"),
        Affirmation("My energy creates my results"),
        Affirmation("I am unstoppable when focused"),
        Affirmation("Every day brings new possibilities"),
        Affirmation("I cultivate powerful habits daily"),
        Affirmation("My determination knows no limits"),
        Affirmation("I breathe in fresh motivation"),
        Affirmation("Small steps create big changes"),
        Affirmation("I design my perfect day"),
        Affirmation("My willpower grows stronger daily"),
        Affirmation("I commit to my goals"),
        Affirmation("My actions match my ambitions"),
        Affirmation("I create my own luck"),
        Affirmation("Today I exceed my expectations"),
        Affirmation("I am focused and disciplined"),
        Affirmation("My productivity fuels my purpose"),
        Affirmation("I overcome procrastination with action"),
        Affirmation("My work ethic inspires others"),
        Affirmation("I finish what I start"),
        Affirmation("Clarity guides all my actions"),
        Affirmation("I invest in my growth"),
        Affirmation("Excellence is my daily standard"),
        Affirmation("I prioritize what truly matters"),
        Affirmation("My passion drives my performance"),
        Affirmation("I take charge right now"),
        Affirmation("My potential expands every day"),
        Affirmation("I build momentum with action"),
        Affirmation("Discipline equals freedom for me"),
        Affirmation("I am a problem solver"),
        Affirmation("My hustle has no off-season"),
        Affirmation("I create before I consume"),
        Affirmation("Today I maximize every moment"),
        Affirmation("I replace doubt with determination"),
        Affirmation("My work expresses my values"),
        Affirmation("I am resilient and resourceful"),
        Affirmation("Progress excites and motivates me"),
        Affirmation("I am in control today"),
        Affirmation("My effort compounds over time"),
        Affirmation("I embrace necessary hard work"),
        Affirmation("My focus cannot be broken"),
        Affirmation("I show up consistently powerfully"),
        Affirmation("Productivity is my natural state"),
        Affirmation("I choose growth over comfort"),
        Affirmation("My mind is productivity focused"),
        Affirmation("I attract productive energy today"),
        Affirmation("Each task brings me closer"),
        Affirmation("I am a momentum builder"),
        Affirmation("My discipline shapes my destiny"),
        Affirmation("I act despite how I feel"),
        Affirmation("My work ethic is legendary"),
        Affirmation("I focus on the process"),
        Affirmation("Today I break personal records"),
        Affirmation("I build my dream daily"),
        Affirmation("My consistency creates spectacular results"),
        Affirmation("I am becoming my best self"),
        Affirmation("Action is my antidote to anxiety"),
        Affirmation("I am powerfully self-motivated"),
        Affirmation("My goals demand my attention"),
        Affirmation("I honor my commitments today"),
        Affirmation("Productivity flows through me easily"),
        Affirmation("I turn intention into action"),
        Affirmation("My habits support my dreams"),
        Affirmation("I take massive action now"),
        Affirmation("Every effort moves me forward"),
        Affirmation("I am the driver here"),
        Affirmation("My focus is razor sharp"),
        Affirmation("I thrive under healthy pressure"),
        Affirmation("Today I choose powerful action"),
        Affirmation("I complete tasks with excellence"),
        Affirmation("My mindset attracts productive opportunities"),
        Affirmation("I am a high-performance individual"),
        Affirmation("My work today matters immensely"),
        Affirmation("I embrace the grind joyfully"),
        Affirmation("Results follow my consistent effort"),
        Affirmation("I am fiercely self-disciplined"),
        Affirmation("Today I build my legacy"),
        Affirmation("My energy is focused productive"),
        Affirmation("I prioritize execution over perfection"),
        Affirmation("Each hour I make progress"),
        Affirmation("I control my focus completely"),
        Affirmation("My determination overcomes all obstacles"),
        Affirmation("I am a productive force"),
        Affirmation("Today I create meaningful value"),
        Affirmation("My discipline is my superpower"),
        Affirmation("I act with urgency today"),
        Affirmation("My potential unfolds through work"),
        Affirmation("I convert ideas into reality"),
        Affirmation("Today I maximize my potential"),
        Affirmation("I am relentlessly action oriented"),
        Affirmation("My productivity inspires everyone around"),
        Affirmation("I break through mental barriers"),
        Affirmation("Consistent action brings desired results"),
        Affirmation("I design my productive environment"),
        Affirmation("My focus brings financial freedom"),
        Affirmation("I show up when tired"),
        Affirmation("Every task teaches me something"),
        Affirmation("I build my skills daily"),
        Affirmation("My work has deep purpose"),
        Affirmation("I honor my time completely"),
        Affirmation("Procrastination has no power here"),
        Affirmation("I choose productive thoughts daily"),
        Affirmation("My actions create my future"),
        Affirmation("I am an unstoppable achiever"),
        Affirmation("Today I outwork my doubts"),
        Affirmation("My willpower strengthens with use"),
        Affirmation("I attract success through persistence"),
        Affirmation("Productivity is my chosen path"),
        Affirmation("I build empires with consistency"),
        Affirmation("My focus attracts abundant opportunities"),
        Affirmation("I embrace challenging meaningful work"),
        Affirmation("Every completed task energizes me"),
        Affirmation("I am a productivity magnet"),
        Affirmation("My effort today builds tomorrow"),
        Affirmation("I transform pressure into performance"),
        Affirmation("Clarity precedes my productive action"),
        Affirmation("I am intrinsically motivated always"),
        Affirmation("My hustle creates my happiness"),
        Affirmation("I turn dreams into plans"),
        Affirmation("Action is my favorite language"),
        Affirmation("I am a focused executor"),
        Affirmation("My discipline creates beautiful freedom"),
        Affirmation("I choose work over distraction"),
        Affirmation("Every moment is productive opportunity"),
        Affirmation("I build wealth through work"),
        Affirmation("My persistence breaks all resistance"),
        Affirmation("I master my time daily"),
        Affirmation("Productivity is my competitive advantage"),
        Affirmation("I am a relentless implementer"),
        Affirmation("My actions speak my intentions"),
        Affirmation("I finish strong every day"),
        Affirmation("Consistency is my secret weapon"),
        Affirmation("I operate at peak performance"),
        Affirmation("My work transforms my world"),
        Affirmation("I attract productive collaborations today"),
        Affirmation("Discipline makes everything easier"),
        Affirmation("I am a proactive creator"),
        Affirmation("My focus unlocks hidden potentials"),
        Affirmation("I value progress over perfection"),
        Affirmation("Each action builds my confidence"),
        Affirmation("I convert time into achievements"),
        Affirmation("My productivity is non-negotiable"),
        Affirmation("I embrace the work required"),
        Affirmation("Success is my daily habit"),
        Affirmation("I am a momentum machine"),
        Affirmation("My effort compounds magnificently"),
        Affirmation("I choose powerful productive habits"),
        Affirmation("Today I build my dream"),
        Affirmation("I am a disciplined visionary"),
        Affirmation("My work ethic opens doors"),
        Affirmation("I focus on impact"),
        Affirmation("Productivity flows from my purpose"),
        Affirmation("I take ownership completely"),
        Affirmation("My actions create massive value"),
        Affirmation("I am a productive powerhouse"),
        Affirmation("Every day I get better"),
        Affirmation("I build my focus muscle"),
        Affirmation("My consistency creates mastery"),
        Affirmation("I choose work that matters"),
        Affirmation("Today I create exceptional work"),
        Affirmation("I am a focused builder"),
        Affirmation("My discipline shapes my character"),
        Affirmation("I overcome all resistance today"),
        Affirmation("Productivity is my natural rhythm"),
        Affirmation("I attract wealth through work"),
        Affirmation("My effort inspires my family"),
        Affirmation("I turn plans into reality"),
        Affirmation("Today I break through limits"),
        Affirmation("I am a consistent performer"),
        Affirmation("My focus creates financial abundance"),
        Affirmation("I embrace productive discomfort daily"),
        Affirmation("Action is my meditation"),
        Affirmation("I build my future now"),
        Affirmation("My work changes lives"),
        Affirmation("I choose discipline daily"),
        Affirmation("Every task has purpose"),
        Affirmation("I am a productive leader"),
        Affirmation("My consistency builds unstoppable momentum"),
        Affirmation("I focus on execution"),
        Affirmation("Today I create massive value"),
        Affirmation("I am a results machine"),
        Affirmation("My effort never goes wasted"),
        Affirmation("I build with focused intensity"),
        Affirmation("Productivity is my love language"),
        Affirmation("I attract success through work"),
        Affirmation("My discipline creates time freedom"),
        Affirmation("I choose meaningful action"),
        Affirmation("Every hour I make progress"),
        Affirmation("I am a focused achiever"),
        Affirmation("My work builds my legacy"),
        Affirmation("I embrace the necessary grind"),
        Affirmation("Action cures all fear"),
        Affirmation("I build wealth daily"),
        Affirmation("My focus attracts success"),
        Affirmation("I choose productive thoughts"),
        Affirmation("Today I exceed expectations"),
        Affirmation("I am a disciplined executor"),
        Affirmation("My consistency creates opportunities"),
        Affirmation("I focus on value creation"),
        Affirmation("Productivity is my superpower"),
        Affirmation("I attract abundance through action"),
        Affirmation("My effort builds my future"),
        Affirmation("I turn goals into reality"),
        Affirmation("Today I break records"),
        Affirmation("I am a focused creator"),
        Affirmation("My discipline equals freedom"),
        Affirmation("I embrace hard work"),
        Affirmation("Action builds my confidence"),
        Affirmation("I build my empire"),
        Affirmation("My focus creates miracles"),
        Affirmation("I choose work over entertainment"),
        Affirmation("Every task moves me forward"),
        Affirmation("I am a productive force"),
        Affirmation("My consistency is legendary"),
        Affirmation("I focus on progress"),
        Affirmation("Today I create excellence"),
        Affirmation("I am a disciplined builder"),
        Affirmation("My work changes everything"),
        Affirmation("I attract success consistently"),
        Affirmation("My effort inspires others"),
        Affirmation("I turn dreams into action"),
        Affirmation("Today I build greatness"),
        Affirmation("I am a focused winner"),
        Affirmation("My discipline creates abundance"),
        Affirmation("I embrace productive challenges"),
        Affirmation("Action is my identity"),
        Affirmation("I build my dreams"),
        Affirmation("My focus attracts wealth"),
        Affirmation("I choose consistent action"),
        Affirmation("Every moment I create"),
        Affirmation("I am a productive champion"),
        Affirmation("My consistency builds empires"),
        Affirmation("I focus on results"),
        Affirmation("Today I make history"),
        Affirmation("I am a disciplined champion"),
        Affirmation("My work inspires generations"),
        Affirmation("I attract opportunities through work"),
        Affirmation("My effort creates freedom"),
        Affirmation("I turn visions into reality"),
        Affirmation("Today I break barriers"),
        Affirmation("I am a focused legend"),
        Affirmation("My discipline shapes destiny"),
        Affirmation("I embrace the work"),
        Affirmation("Action creates my reality"),
        Affirmation("I build my kingdom"),
        Affirmation("My focus creates abundance"),
        Affirmation("I choose productive action"),
        Affirmation("Every day I build"),
        Affirmation("I am a productive warrior"),
        Affirmation("My consistency creates wealth"),
        Affirmation("I focus on creation"),
        Affirmation("Today I change everything"),
        Affirmation("I am a disciplined warrior"),
        Affirmation("My work matters immensely"),
        Affirmation("I attract greatness through work"),
        Affirmation("My effort builds character"),
        Affirmation("I turn ideas into impact"),
        Affirmation("Today I build mastery"),
        Affirmation("I am a focused builder"),
        Affirmation("My discipline creates excellence"),
        Affirmation("I embrace daily grind"),
        Affirmation("Action fuels my soul"),
        Affirmation("I build my vision"),
        Affirmation("My focus attracts opportunities"),
        Affirmation("I choose powerful work"),
        Affirmation("Every action creates value"),
        Affirmation("I am a productive master"),
        Affirmation("My consistency creates impact"),
        Affirmation("I focus on contribution"),
        Affirmation("Today I create magic"),
        Affirmation("I am a disciplined master"),
        Affirmation("My work changes me"),
        Affirmation("I attract success daily"),
        Affirmation("My effort shapes reality"),
        Affirmation("I turn passion into productivity"),
        Affirmation("Today I build excellence"),
        Affirmation("I am a focused master"),
        Affirmation("My discipline creates results"),
        Affirmation("I embrace creative work"),
        Affirmation("Action builds my legacy"),
        Affirmation("I build my success"),
        Affirmation("My focus creates impact"),
        Affirmation("I choose inspired action"),
        Affirmation("Every task builds me"),
        Affirmation("I am a productive artist"),
        Affirmation("My consistency creates mastery"),
        Affirmation("I focus on innovation"),
        Affirmation("Today I build value"),
        Affirmation("I am a disciplined artist"),
        Affirmation("My work expresses love"),
        Affirmation("I attract creative opportunities"),
        Affirmation("My effort creates beauty"),
        Affirmation("I turn energy into achievement"),
        Affirmation("Today I create art"),
        Affirmation("I am a focused artist"),
        Affirmation("My discipline creates beauty"),
        Affirmation("I embrace the process"),
        Affirmation("Action expresses my values"),
        Affirmation("I build meaningful things"),
        Affirmation("My focus attracts inspiration"),
        Affirmation("I choose creative work"),
        Affirmation("Every creation matters"),
        Affirmation("I am a productive genius"),
        Affirmation("My consistency creates art"),
        Affirmation("I focus on expression"),
        Affirmation("Today I inspire others"),
        Affirmation("I am a disciplined genius"),
        Affirmation("My work touches souls"),
        Affirmation("I attract divine inspiration"),
        Affirmation("My effort creates harmony"),
        Affirmation("I turn thoughts into creations"),
        Affirmation("Today I build wonder"),
        Affirmation("I am a focused genius"),
        Affirmation("My discipline creates wonder"),
        Affirmation("I embrace inspired action"),
        Affirmation("Action creates beauty"),
        Affirmation("I build inspiring things"),
        Affirmation("My focus creates masterpieces"),
        Affirmation("I choose artistic discipline"),
        Affirmation("Every creation inspires"),
        Affirmation("I am a productive visionary"),
        Affirmation("My consistency creates legacy"),
        Affirmation("I focus on imagination"),
        Affirmation("Today I create wonders"),
        Affirmation("I am a disciplined visionary"),
        Affirmation("My work heals others"),
        Affirmation("I attract visionary opportunities"),
        Affirmation("My effort creates change"),
        Affirmation("I turn dreams into wonders"),
        Affirmation("Today I build dreams"),
        Affirmation("I am a focused visionary"),
        Affirmation("My discipline creates visions"),
        Affirmation("I embrace visionary work"),
        Affirmation("Action creates future"),
        Affirmation("I build new worlds"),
        Affirmation("My focus creates innovations"),
        Affirmation("I choose groundbreaking work"),
        Affirmation("Every vision becomes real"),
        Affirmation("I am a productive pioneer"),
        Affirmation("My consistency breaks boundaries"),
        Affirmation("I focus on discovery"),
        Affirmation("Today I innovate"),
        Affirmation("I am a disciplined pioneer"),
        Affirmation("My work advances humanity"),
        Affirmation("I attract pioneering opportunities"),
        Affirmation("My effort creates breakthroughs"),
        Affirmation("I turn curiosity into progress"),
        Affirmation("Today I discover new"),
        Affirmation("I am a focused pioneer"),
        Affirmation("My discipline creates progress"),
        Affirmation("I embrace exploration"),
        Affirmation("Action discovers truth"),
        Affirmation("I build new paths"),
        Affirmation("My focus creates discoveries"),
        Affirmation("I choose exploration daily"),
        Affirmation("Every discovery matters"),
        Affirmation("I am a productive explorer"),
        Affirmation("My consistency reveals truths"),
        Affirmation("I focus on learning"),
        Affirmation("Today I explore deeply"),
        Affirmation("I am a disciplined explorer"),
        Affirmation("My work expands knowledge"),
        Affirmation("I attract learning opportunities"),
        Affirmation("My effort creates understanding"),
        Affirmation("I turn questions into answers"),
        Affirmation("Today I learn something"),
        Affirmation("I am a focused explorer"),
        Affirmation("My discipline creates knowledge"),
        Affirmation("I embrace learning daily"),
        Affirmation("Action teaches me"),
        Affirmation("I build understanding"),
        Affirmation("My focus creates wisdom"),
        Affirmation("I choose to learn"),
        Affirmation("Every lesson improves me"),
        Affirmation("I am a productive student"),
        Affirmation("My consistency builds expertise"),
        Affirmation("I focus on growth"),
        Affirmation("Today I improve skills"),
        Affirmation("I am a disciplined student"),
        Affirmation("My work develops mastery"),
        Affirmation("I attract growth opportunities"),
        Affirmation("My effort creates skill"),
        Affirmation("I turn practice into excellence"),
        Affirmation("Today I master something"),
        Affirmation("I am a focused student"),
        Affirmation("My discipline creates expertise"),
        Affirmation("I embrace skill development"),
        Affirmation("Action makes me expert"),
        Affirmation("I build my capabilities"),
        Affirmation("My focus creates mastery"),
        Affirmation("I choose skill development"),
        Affirmation("Every skill empowers me"),
        Affirmation("I am a productive athlete"),
        Affirmation("My consistency creates strength"),
        Affirmation("I focus on fitness"),
        Affirmation("Today I train hard"),
        Affirmation("I am a disciplined athlete"),
        Affirmation("My work builds health"),
        Affirmation("I attract fitness opportunities"),
        Affirmation("My effort creates vitality"),
        Affirmation("I turn exercise into energy"),
        Affirmation("Today I push limits"),
        Affirmation("I am a focused athlete"),
        Affirmation("My discipline creates health"),
        Affirmation("I embrace physical training"),
        Affirmation("Action strengthens my body"),
        Affirmation("I build physical resilience"),
        Affirmation("My focus creates endurance"),
        Affirmation("I choose healthy habits"),
        Affirmation("Every workout transforms me"),
        Affirmation("I am a productive healer"),
        Affirmation("My consistency creates wellness"),
        Affirmation("I focus on wellbeing"),
        Affirmation("Today I nurture myself"),
        Affirmation("I am a disciplined healer"),
        Affirmation("My work restores balance"),
        Affirmation("I attract healing opportunities"),
        Affirmation("My effort creates harmony"),
        Affirmation("I turn care into health"),
        Affirmation("Today I heal completely"),
        Affirmation("I am a focused healer"),
        Affirmation("My discipline creates balance"),
        Affirmation("I embrace self-care daily"),
        Affirmation("Action heals my being"),
        Affirmation("I build holistic health"),
        Affirmation("My focus creates peace"),
        Affirmation("I choose wellness practices"),
        Affirmation("Every healing action matters"),
        Affirmation("I am a productive teacher"),
        Affirmation("My consistency creates understanding"),
        Affirmation("I focus on sharing"),
        Affirmation("Today I enlighten others"),
        Affirmation("I am a disciplined teacher"),
        Affirmation("My work educates many"),
        Affirmation("I attract teaching opportunities"),
        Affirmation("My effort creates wisdom"),
        Affirmation("I turn knowledge into lessons"),
        Affirmation("Today I inspire learning"),
        Affirmation("I am a focused teacher"),
        Affirmation("My discipline creates enlightenment"),
        Affirmation("I embrace educational moments"),
        Affirmation("Action shares my knowledge"),
        Affirmation("I build understanding in others"),
        Affirmation("My focus creates clarity"),
        Affirmation("I choose to teach"),
        Affirmation("Every lesson helps someone"),
        Affirmation("I am a productive giver"),
        Affirmation("My consistency creates abundance"),
        Affirmation("I focus on service"),
        Affirmation("Today I help others"),
        Affirmation("I am a disciplined giver"),
        Affirmation("My work serves humanity"),
        Affirmation("I attract service opportunities"),
        Affirmation("My effort creates joy"),
        Affirmation("I turn compassion into action"),
        Affirmation("Today I make difference"),
        Affirmation("I am a focused giver"),
        Affirmation("My discipline creates generosity"),
        Affirmation("I embrace giving daily"),
        Affirmation("Action serves my purpose"),
        Affirmation("I build a better world"),
        Affirmation("My focus creates kindness"),
        Affirmation("I choose to serve"),
        Affirmation("Every gift matters deeply"),
        Affirmation("I am a productive leader"),
        Affirmation("My consistency creates influence"),
        Affirmation("I focus on guidance"),
        Affirmation("Today I lead powerfully"),
        Affirmation("I am a disciplined leader"),
        Affirmation("My work inspires teams"),
        Affirmation("I attract leadership opportunities"),
        Affirmation("My effort creates direction"),
        Affirmation("I turn vision into leadership"),
        Affirmation("Today I guide others"),
        Affirmation("I am a focused leader"),
        Affirmation("My discipline creates impact"),
        Affirmation("I embrace leadership responsibilities"),
        Affirmation("Action leads to results"),
        Affirmation("I build effective teams"),
        Affirmation("My focus creates success"),
        Affirmation("I choose to lead"),
        Affirmation("Every decision helps many"),
        Affirmation("I am a productive innovator"),
        Affirmation("My consistency creates change"),
        Affirmation("I focus on improvement"),
        Affirmation("Today I create solutions"),
        Affirmation("I am a disciplined innovator"),
        Affirmation("My work solves problems"),
        Affirmation("I attract innovative opportunities"),
        Affirmation("My effort creates progress"),
        Affirmation("I turn challenges into innovations"),
        Affirmation("Today I improve something"),
        Affirmation("I am a focused innovator"),
        Affirmation("My discipline creates solutions"),
        Affirmation("I embrace creative thinking"),
        Affirmation("Action implements my ideas"),
        Affirmation("I build better systems"),
        Affirmation("My focus creates efficiency"),
        Affirmation("I choose to innovate"),
        Affirmation("Every innovation helps"),
        Affirmation("I am a productive communicator"),
        Affirmation("My consistency creates connections"),
        Affirmation("I focus on understanding"),
        Affirmation("Today I communicate clearly"),
        Affirmation("I am a disciplined communicator"),
        Affirmation("My work bridges gaps"),
        Affirmation("I attract communication opportunities"),
        Affirmation("My effort creates harmony"),
        Affirmation("I turn words into connections"),
        Affirmation("Today I connect meaningfully"),
        Affirmation("I am a focused communicator"),
        Affirmation("My discipline creates clarity"),
        Affirmation("I embrace honest dialogue"),
        Affirmation("Action builds relationships"),
        Affirmation("I build strong networks"),
        Affirmation("My focus creates understanding"),
        Affirmation("I choose clear communication"),
        Affirmation("Every conversation matters"),
        Affirmation("I am a productive listener"),
        Affirmation("My consistency creates empathy"),
        Affirmation("I focus on hearing"),
        Affirmation("Today I listen deeply"),
        Affirmation("I am a disciplined listener"),
        Affirmation("My work understands others"),
        Affirmation("I attract listening opportunities"),
        Affirmation("My effort creates connection"),
        Affirmation("I turn attention into insight"),
        Affirmation("Today I hear completely"),
        Affirmation("I am a focused listener"),
        Affirmation("My discipline creates compassion"),
        Affirmation("I embrace attentive listening"),
        Affirmation("Action shows I care"),
        Affirmation("I build empathetic connections"),
        Affirmation("My focus creates trust"),
        Affirmation("I choose to listen"),
        Affirmation("Every listening moment connects")
    )

    init {
        viewModelScope.launch {
            val streak = settingsDataStore.streakCount.first()
            val lastCompletion = settingsDataStore.lastCompletionDate.first()
            val graceDaysUsed = settingsDataStore.graceDaysUsedThisWeek.first()
            val weekStart = settingsDataStore.weekStartDate.first()
            val unlockedAchievements = settingsDataStore.unlockedAchievements.first()

            updateStreakStatus(streak, lastCompletion, graceDaysUsed, weekStart)
            
            _uiState.update {
                it.copy(
                    requiredAffirmation = getAffirmationForToday(),
                    isAffirmationCompletedToday = isToday(lastCompletion),
                    streakCount = settingsDataStore.streakCount.first(),
                    achievements = getUpdatedAchievements(unlockedAchievements)
                )
            }
            scheduleStreakReminderWorker()
        }
    }

    fun onUserTyped(typedText: String) {
        if (typedText.length <= _uiState.value.userTypedAffirmation.length + 1) {
            _uiState.update { it.copy(userTypedAffirmation = typedText, affirmationState = AffirmationState.PENDING) }
        }
    }

    fun completeAffirmation() {
        val required = _uiState.value.requiredAffirmation.sentence.trim().lowercase()
        val typed = _uiState.value.userTypedAffirmation.trim().lowercase()

        if (required == typed) {
            viewModelScope.launch {
                _uiState.update { it.copy(affirmationState = AffirmationState.CORRECT) }
                delay(1500) // Wait for animation

                val streak = settingsDataStore.streakCount.first() + 1
                val now = System.currentTimeMillis()
                val graceDays = settingsDataStore.graceDaysUsedThisWeek.first()
                val weekStart = settingsDataStore.weekStartDate.first()

                settingsDataStore.saveStreakData(streak, now, graceDays, weekStart)

                val newlyUnlocked = checkAndUnlockAchievements(streak)

                _uiState.update {
                    it.copy(
                        isAffirmationCompletedToday = true,
                        streakCount = streak,
                        achievements = getUpdatedAchievements(settingsDataStore.unlockedAchievements.first())
                    )
                }

                newlyUnlocked.forEach { achievement ->
                    notificationHelper.showAchievementNotification(achievement)
                }
            }
        } else {
            _uiState.update { it.copy(affirmationState = AffirmationState.INCORRECT) }
        }
    }

    private fun getAffirmationForToday(): Affirmation {
        val dayOfYear = Calendar.getInstance().get(Calendar.DAY_OF_YEAR)
        return affirmations[dayOfYear % affirmations.size]
    }

    private fun isToday(timestamp: Long): Boolean {
        if (timestamp == 0L) return false
        val today = Calendar.getInstance()
        val other = Calendar.getInstance().apply { timeInMillis = timestamp }
        return today.get(Calendar.YEAR) == other.get(Calendar.YEAR) &&
               today.get(Calendar.DAY_OF_YEAR) == other.get(Calendar.DAY_OF_YEAR)
    }

    private suspend fun updateStreakStatus(currentStreak: Int, lastCompletion: Long, graceDaysUsed: Int, weekStart: Long) {
        if (isToday(lastCompletion) || currentStreak == 0) return
        val now = System.currentTimeMillis()
        val daysSinceLastCompletion = TimeUnit.MILLISECONDS.toDays(now - lastCompletion)
        var newStreak = currentStreak
        var newGraceDays = graceDaysUsed
        var newWeekStart = weekStart
        if (now - weekStart > TimeUnit.DAYS.toMillis(7)) {
            newWeekStart = now
            newGraceDays = 0
        }
        if (daysSinceLastCompletion > 1) {
            val daysToPenalize = daysSinceLastCompletion - 1
            if (daysToPenalize > (1 - newGraceDays)) {
                newStreak = 0
            } else {
                newGraceDays += daysToPenalize.toInt()
            }
        }
        settingsDataStore.saveStreakData(newStreak, lastCompletion, newGraceDays, newWeekStart)
    }

    private suspend fun checkAndUnlockAchievements(currentStreak: Int): List<Achievement> {
        val currentlyUnlocked = settingsDataStore.unlockedAchievements.first()
        val newlyUnlocked = mutableListOf<Achievement>()
        for (achievement in AchievementsList.allAchievements) {
            if (currentStreak >= achievement.streakRequired && !currentlyUnlocked.contains(achievement.id)) {
                newlyUnlocked.add(achievement)
            }
        }
        if (newlyUnlocked.isNotEmpty()) {
            val allUnlockedIds = currentlyUnlocked + newlyUnlocked.map { it.id }
            settingsDataStore.saveUnlockedAchievements(allUnlockedIds)
        }
        return newlyUnlocked
    }

    private fun getUpdatedAchievements(unlockedIds: Set<String>): List<Achievement> {
        return AchievementsList.allAchievements.map {
            it.copy(isUnlocked = unlockedIds.contains(it.id))
        }
    }

    private fun scheduleStreakReminderWorker() {
        val workRequest = PeriodicWorkRequestBuilder<StreakReminderWorker>(24, TimeUnit.HOURS).build()
        workManager.enqueueUniquePeriodicWork("StreakReminderWork", ExistingPeriodicWorkPolicy.KEEP, workRequest)
    }
}
