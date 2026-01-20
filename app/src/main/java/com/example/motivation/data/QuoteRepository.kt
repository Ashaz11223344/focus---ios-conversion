package com.example.motivation.data

object QuoteRepository {
    val allQuotes = listOf(
        // Original quotes
        "Believe you can and you're halfway there.",
        "The best way to predict the future is to create it.",
        "The only way to do great work is to love what you do.",
        "Success is not final, failure is not fatal: it is the courage to continue that counts.",
        "The future belongs to those who believe in the beauty of their dreams.",
        "The only limit to our realization of tomorrow will be our doubts of today.",
        "It does not matter how slowly you go as long as you do not stop.",
        "Everything you've ever wanted is on the other side of fear.",
        "The journey of a thousand miles begins with a single step.",
        "Your time is limited, don't waste it living someone else's life.",

        // Wisdom & Philosophy
        "Know thyself.",
        "The unexamined life is not worth living.",
        "To be yourself in a world that is constantly trying to make you something else is the greatest accomplishment.",
        "Life is what happens to you while you're busy making other plans.",
        "In three words I can sum up everything I've learned about life: it goes on.",
        "The only true wisdom is in knowing you know nothing.",
        "What we think, we become.",
        "Happiness depends upon ourselves.",
        "He who has a why to live can bear almost any how.",
        "The mind is everything. What you think you become.",

        // Success & Achievement
        "Opportunities don't happen. You create them.",
        "Don't be afraid to give up the good to go for the great.",
        "I find that the harder I work, the more luck I seem to have.",
        "Success is walking from failure to failure with no loss of enthusiasm.",
        "The way to get started is to quit talking and begin doing.",
        "Don't let yesterday take up too much of today.",
        "It's not whether you get knocked down, it's whether you get up.",
        "The only place where success comes before work is in the dictionary.",
        "Success usually comes to those who are too busy to be looking for it.",
        "There are no shortcuts to any place worth going.",

        // Courage & Strength
        "Courage is resistance to fear, mastery of fear, not absence of fear.",
        "You gain strength, courage, and confidence by every experience in which you really stop to look fear in the face.",
        "We must embrace pain and burn it as fuel for our journey.",
        "Strength does not come from physical capacity. It comes from an indomitable will.",
        "What lies behind us and what lies before us are tiny matters compared to what lies within us.",
        "Be bold and courageous. When you look back on your life, you'll regret the things you didn't do more than the ones you did.",
        "Life is either a daring adventure or nothing at all.",
        "You must do the things you think you cannot do.",
        "Courage doesn't always roar. Sometimes courage is the quiet voice at the end of the day saying, 'I will try again tomorrow.'",
        "The brave man is not he who does not feel afraid, but he who conquers that fear.",

        // Love & Relationships
        "The greatest happiness of life is the conviction that we are loved; loved for ourselves, or rather, loved in spite of ourselves.",
        "Love is composed of a single soul inhabiting two bodies.",
        "We loved with a love that was more than love.",
        "To love and be loved is to feel the sun from both sides.",
        "Love is that condition in which the happiness of another person is essential to your own.",
        "The best thing to hold onto in life is each other.",
        "Love is friendship that has caught fire.",
        "Being deeply loved by someone gives you strength, while loving someone deeply gives you courage.",
        "Love is when the other person's happiness is more important than your own.",
        "A successful marriage requires falling in love many times, always with the same person.",

        // Creativity & Innovation
        "Creativity is intelligence having fun.",
        "Innovation distinguishes between a leader and a follower.",
        "The creative process is not controlled by a switch you can simply turn on or off; it's with you all the time.",
        "Every artist was first an amateur.",
        "Creativity takes courage.",
        "Imagination is more important than knowledge.",
        "To live a creative life, we must lose our fear of being wrong.",
        "The worst enemy to creativity is self-doubt.",
        "Don't think. Thinking is the enemy of creativity.",
        "Creativity is contagious, pass it on.",

        // Perseverance & Determination
        "Perseverance is not a long race; it is many short races one after the other.",
        "Fall seven times, stand up eight.",
        "Our greatest glory is not in never falling, but in rising every time we fall.",
        "Energy and persistence conquer all things.",
        "It always seems impossible until it's done.",
        "The difference between the impossible and the possible lies in a person's determination.",
        "When you come to the end of your rope, tie a knot and hang on.",
        "Perseverance is failing 19 times and succeeding the 20th.",
        "Victory belongs to the most persevering.",
        "A river cuts through rock, not because of its power, but because of its persistence.",

        // Happiness & Contentment
        "Happiness is not something ready made. It comes from your own actions.",
        "The purpose of our lives is to be happy.",
        "Happiness is when what you think, what you say, and what you do are in harmony.",
        "Be happy for this moment. This moment is your life.",
        "The secret of happiness is freedom, the secret of freedom is courage.",
        "Happiness is a butterfly, which when pursued, is always just beyond your grasp, but which, if you will sit down quietly, may alight upon you.",
        "Count your age by friends, not years. Count your life by smiles, not tears.",
        "The happiest people don't have the best of everything, they make the best of everything.",
        "Happiness is not a station you arrive at, but a manner of traveling.",
        "For every minute you are angry you lose sixty seconds of happiness.",

        // Learning & Growth
        "Live as if you were to die tomorrow. Learn as if you were to live forever.",
        "The capacity to learn is a gift; the ability to learn is a skill; the willingness to learn is a choice.",
        "Education is the most powerful weapon which you can use to change the world.",
        "The beautiful thing about learning is that no one can take it away from you.",
        "An investment in knowledge pays the best interest.",
        "Change is the end result of all true learning.",
        "Learning never exhausts the mind.",
        "The more that you read, the more things you will know. The more that you learn, the more places you'll go.",
        "Education is not the filling of a pail, but the lighting of a fire.",
        "Anyone who stops learning is old, whether at twenty or eighty. Anyone who keeps learning stays young.",

        // Time & Legacy
        "Time you enjoy wasting is not wasted time.",
        "The two most powerful warriors are patience and time.",
        "Lost time is never found again.",
        "Time is what we want most, but what we use worst.",
        "The key is in not spending time, but in investing it.",
        "Don't count the days, make the days count.",
        "Time is the most valuable thing a man can spend.",
        "The present time has one advantage over every other - it is our own.",
        "Your time is limited, so don't waste it living someone else's life.",
        "The future depends on what you do today.",

        // Inspiration & Motivation
        "Start where you are. Use what you have. Do what you can.",
        "The only way to achieve the impossible is to believe it is possible.",
        "You are never too old to set another goal or to dream a new dream.",
        "What you get by achieving your goals is not as important as what you become by achieving your goals.",
        "Limitations live only in our minds. But if we use our imaginations, our possibilities become limitless.",
        "The secret of getting ahead is getting started.",
        "Don't watch the clock; do what it does. Keep going.",
        "A year from now you may wish you had started today.",
        "The only person you are destined to become is the person you decide to be.",
        "Dream big and dare to fail.",

        // Leadership & Influence
        "Leadership is not about being in charge. It is about taking care of those in your charge.",
        "A leader is one who knows the way, goes the way, and shows the way.",
        "The greatest leader is not necessarily the one who does the greatest things. He is the one that gets the people to do the greatest things.",
        "If your actions inspire others to dream more, learn more, do more and become more, you are a leader.",
        "The art of leadership is saying no, not yes. It is very easy to say yes.",
        "Before you are a leader, success is all about growing yourself. When you become a leader, success is all about growing others.",
        "Leadership is the capacity to translate vision into reality.",
        "A good leader takes a little more than his share of the blame, a little less than his share of the credit.",
        "The function of leadership is to produce more leaders, not more followers.",
        "Leadership is not about titles, positions or flowcharts. It is about one life influencing another.",

        // Nature & Simplicity
        "In every walk with nature one receives far more than he seeks.",
        "The clearest way into the Universe is through a forest wilderness.",
        "Look deep into nature, and then you will understand everything better.",
        "Nature does not hurry, yet everything is accomplished.",
        "Adopt the pace of nature: her secret is patience.",
        "The poetry of the earth is never dead.",
        "To sit in the shade on a fine day and look upon verdure is the most perfect refreshment.",
        "Study nature, love nature, stay close to nature. It will never fail you.",
        "Nature is not a place to visit. It is home.",
        "The earth has music for those who listen.",

        // Health & Well-being
        "Health is the greatest gift, contentment the greatest wealth, faithfulness the best relationship.",
        "The groundwork of all happiness is health.",
        "To keep the body in good health is a duty... otherwise we shall not be able to keep our mind strong and clear.",
        "Health is a state of complete harmony of the body, mind and spirit.",
        "The first wealth is health.",
        "A healthy outside starts from the inside.",
        "Take care of your body. It's the only place you have to live.",
        "Health is not valued until sickness comes.",
        "He who has health has hope; and he who has hope has everything.",
        "Physical fitness is not only one of the most important keys to a healthy body, it is the basis of dynamic and creative intellectual activity.",

        // Friendship
        "A friend is one that knows you as you are, understands where you have been, accepts what you have become, and still, gently allows you to grow.",
        "Friendship is born at that moment when one person says to another, 'What! You too? I thought I was the only one.'",
        "True friendship comes when the silence between two people is comfortable.",
        "A real friend is one who walks in when the rest of the world walks out.",
        "Friendship is the only cement that will ever hold the world together.",
        "A single rose can be my garden... a single friend, my world.",
        "There is nothing on this earth more to be prized than true friendship.",
        "Friends show their love in times of trouble, not in happiness.",
        "The language of friendship is not words but meanings.",
        "Friendship is the golden thread that ties the heart of all the world.",

        // Change & Adaptation
        "The only constant in life is change.",
        "Change your thoughts and you change your world.",
        "Progress is impossible without change, and those who cannot change their minds cannot change anything.",
        "If you don't like something, change it. If you can't change it, change your attitude.",
        "Life is a series of natural and spontaneous changes. Don't resist them; that only creates sorrow. Let reality be reality.",
        "To improve is to change; to be perfect is to change often.",
        "Change before you have to.",
        "All great changes are preceded by chaos.",
        "Be the change that you wish to see in the world.",
        "When we are no longer able to change a situation, we are challenged to change ourselves.",

        // Gratitude
        "Gratitude turns what we have into enough, and more.",
        "The more grateful I am, the more beauty I see.",
        "Gratitude is the healthiest of all human emotions.",
        "Gratitude makes sense of our past, brings peace for today, and creates a vision for tomorrow.",
        "Joy is the simplest form of gratitude.",
        "Gratitude is not only the greatest of virtues, but the parent of all others.",
        "When you are grateful, fear disappears and abundance appears.",
        "Gratitude is the wine for the soul. Go on. Get drunk.",
        "Gratitude unlocks the fullness of life.",
        "Appreciation is a wonderful thing: It makes what is excellent in others belong to us as well.",

        // Simplicity & Minimalism
        "Simplicity is the ultimate sophistication.",
        "The ability to simplify means to eliminate the unnecessary so that the necessary may speak.",
        "Perfection is achieved, not when there is nothing more to add, but when there is nothing left to take away.",
        "Simplicity is about subtracting the obvious and adding the meaningful.",
        "Live simply so that others may simply live.",
        "Simplicity is the glory of expression.",
        "Be content with what you have; rejoice in the way things are. When you realize there is nothing lacking, the whole world belongs to you.",
        "The simplest things are often the truest.",
        "Less is more.",
        "Simplicity is the keynote of all true elegance.",

        // Action & Initiative
        "Well begun is half done.",
        "Action is the foundational key to all success.",
        "The path to success is to take massive, determined action.",
        "An ounce of action is worth a ton of theory.",
        "Action may not always bring happiness, but there is no happiness without action.",
        "Take action! An inch of movement will bring you closer to your goals than a mile of intention.",
        "Do not wait; the time will never be 'just right.' Start where you stand, and work with whatever tools you may have.",
        "Inaction breeds doubt and fear. Action breeds confidence and courage.",
        "The world makes way for the man who knows where he is going.",
        "Act as if what you do makes a difference. It does.",

        // Vision & Dreams
        "All our dreams can come true, if we have the courage to pursue them.",
        "The biggest adventure you can take is to live the life of your dreams.",
        "Dream no small dreams for they have no power to move the hearts of men.",
        "Hold fast to dreams, for if dreams die, life is a broken-winged bird that cannot fly.",
        "You see things; and you say, 'Why?' But I dream things that never were; and I say, 'Why not?'",
        "Dreams are illustrations from the book your soul is writing about you.",
        "Follow your dreams, they know the way.",
        "A dream doesn't become reality through magic; it takes sweat, determination and hard work.",
        "The future belongs to those who believe in the beauty of their dreams.",
        "Don't be pushed around by the fears in your mind. Be led by the dreams in your heart.",

        // Resilience
        "The human capacity for burden is like bamboo - far more flexible than you'd ever believe at first glance.",
        "Rock bottom became the solid foundation on which I rebuilt my life.",
        "Resilience is knowing that you are the only one that has the power and the responsibility to pick yourself up.",
        "It's your reaction to adversity, not adversity itself that determines how your life's story will develop.",
        "The oak fought the wind and was broken, the willow bent when it must and survived.",
        "You may have to fight a battle more than once to win it.",
        "Our greatest weakness lies in giving up. The most certain way to succeed is always to try just one more time.",
        "Resilience is accepting your new reality, even if it's less good than the one you had before.",
        "The strongest people are not those who show strength in front of us but those who win battles we know nothing about.",
        "Resilience is very different than being numb. Resilience means you experience, you feel, you fail, you hurt. You fall. But, you keep going.",

        // Humility
        "Humility is not thinking less of yourself, it's thinking of yourself less.",
        "True humility is not thinking less of yourself; it is thinking of yourself less.",
        "Pride makes us artificial and humility makes us real.",
        "The proud man can learn humility, but he will be proud of it.",
        "Humility is the solid foundation of all virtues.",
        "Stay humble. Work hard. Be kind.",
        "Humility is the ability to give up your pride and still retain your dignity.",
        "Knowledge is proud that it knows so much; wisdom is humble that it knows no more.",
        "The higher we are placed, the more humbly we should walk.",
        "Humility is nothing but truth, and pride is nothing but lying.",

        // Purpose & Meaning
        "The purpose of life is not to be happy. It is to be useful, to be honorable, to be compassionate, to have it make some difference that you have lived and lived well.",
        "Life is never made unbearable by circumstances, but only by lack of meaning and purpose.",
        "The meaning of life is to find your gift. The purpose of life is to give it away.",
        "My mission in life is not merely to survive, but to thrive; and to do so with some passion, some compassion, some humor, and some style.",
        "If you can't figure out your purpose, figure out your passion. For your passion will lead you right into your purpose.",
        "The two most important days in your life are the day you are born and the day you find out why.",
        "He who has a why to live can bear almost any how.",
        "Your purpose in life is to find your purpose and give your whole heart and soul to it.",
        "The purpose of human life is to serve, and to show compassion and the will to help others.",
        "A person who has a why can bear any how.",

        // Add thousands more quotes here...
        // Continue adding in the same format
        // Example categories to expand:
        // - Hope & Optimism
        // - Forgiveness
        // - Patience
        // - Money & Wealth
        // - Travel & Adventure
        // - Science & Discovery
        // - Art & Beauty
        // - Music
        // - Technology
        // - History
        // - Family
        // - Spirituality
        // - Work & Career
        // - Education
        // - Communication
        // - Problem Solving
        // - Decision Making
        // - Risk Taking
        // - Freedom
        // - Justice
        // - Peace
        // - Environmentalism
        // - Community
        // - Individuality
        // - Aging
        // - Youth
        // - Laughter & Humor
        // - Silence & Solitude
        // - Balance
        // - Honesty
        // - Trust
        // - Kindness
        // - Compassion
        // - Generosity
        // - Service
        // - Excellence
        // - Quality
        // - Details
        // - Patterns
        // - Systems
        // - Process
        // - Results
        // - Goals
        // - Planning
        // - Preparation
        // - Execution
        // - Review
        // - Improvement
        // - Mastery
        // - Legacy
        // - Memory
        // - Tradition
        // - Innovation
        // - Revolution
        // - Evolution
        // - Transformation
        // - Enlightenment
        // - Wisdom
        // - Truth
        // - Reality
        // - Perception
        // - Perspective
        // - Focus
        // - Concentration
        // - Attention
        // - Awareness
        // - Consciousness
        // - Mindfulness
        // - Presence
        // - Flow
        // - Energy
        // - Vibration
        // - Frequency
        // - Connection
        // - Unity
        // - Oneness
        // - Diversity
        // - Inclusion
        // - Equity
        // - Equality
        // - Fairness
        // - Rights
        // - Responsibilities
        // - Duties
        // - Obligations
        // - Promises
        // - Commitments
        // - Dedication
        // - Devotion
        // - Loyalty
        // - Fidelity
        // - Honor
        // - Integrity
        // - Character
        // - Reputation
        // - Image
        // - Brand
        // - Identity
        // - Self
        // - Ego
        // - Soul
        // - Spirit
        // - Heart
        // - Mind
        // - Body
        // - Emotions
        // - Feelings
        // - Thoughts
        // - Ideas
        // - Concepts
        // - Theories
        // - Principles
        // - Laws
        // - Rules
        // - Guidelines
        // - Standards
        // - Values
        // - Ethics
        // - Morals
        // - Virtues
        // - Vices
        // - Sins
        // - Redemption
        // - Salvation
        // - Liberation
        // - Freedom
        // - Independence
        // - Interdependence
        // - Cooperation
        // - Collaboration
        // - Partnership
        // - Teamwork
        // - Synergy
        // - Harmony
        // - Rhythm
        // - Timing
        // - Pacing
        // - Speed
        // - Velocity
        // - Acceleration
        // - Momentum
        // - Inertia
        // - Force
        // - Power
        // - Strength
        // - Weakness
        // - Vulnerability
        // - Sensitivity
        // - Toughness
        // - Resilience
        // - Flexibility
        // - Adaptability
        // - Versatility
        // - Multiplicity
        // - Complexity
        // - Simplicity
        // - Elegance
        // - Grace
        // - Beauty
        // - Aesthetics
        // - Artistry
        // - Craftsmanship
        // - Skill
        // - Talent
        // - Genius
        // - Gift
        // - Blessing
        // - Curse
        // - Challenge
        // - Difficulty
        // - Struggle
        // - Suffering
        // - Pain
        // - Pleasure
        // - Joy
        // - Delight
        // - Ecstasy
        // - Bliss
        // - Nirvana
        // - Heaven
        // - Hell
        // - Utopia
        // - Dystopia
        // - Reality
        // - Fantasy
        // - Dream
        // - Nightmare
        // - Vision
        // - Hallucination
        // - Illusion
        // - Delusion
        // - Reality
        // - Truth
        // - Lie
        // - Deception
        // - Honesty
        // - Transparency
        // - Clarity
        // - Obscurity
        // - Mystery
        // - Secret
        // - Revelation
        // - Discovery
        // - Invention
        // - Creation
        // - Destruction
        // - Construction
        // - Deconstruction
        // - Reconstruction
        // - Transformation
        // - Metamorphosis
        // - Evolution
        // - Revolution
        // - Change
        // - Stability
        // - Security
        // - Safety
        // - Danger
        // - Risk
        // - Reward
        // - Punishment
        // - Consequence
        // - Result
        // - Outcome
        // - Output
        // - Input
        // - Throughput
        // - Process
        // - System
        // - Structure
        // - Function
        // - Form
        // - Content
        // - Context
        // - Environment
        // - Situation
        // - Circumstance
        // - Condition
        // - State
        // - Status
        // - Position
        // - Rank
        // - Level
        // - Degree
        // - Extent
        // - Scope
        // - Scale
        // - Size
        // - Magnitude
        // - Intensity
        // - Severity
        // - Gravity
        // - Importance
        // - Significance
        // - Relevance
        // - Meaning
        // - Purpose
        // - Goal
        // - Objective
        // - Aim
        // - Target
        // - Destination
        // - Journey
        // - Path
        // - Road
        // - Way
        // - Method
        // - Approach
        // - Strategy
        // - Tactic
        // - Technique
        // - Skill
        // - Ability
        // - Capability
        // - Capacity
        // - Potential
        // - Possibility
        // - Probability
        // - Certainty
        // - Uncertainty
        // - Doubt
        // - Belief
        // - Faith
        // - Trust
        // - Confidence
        // - Assurance
        // - Guarantee
        // - Promise
        // - Commitment
        // - Obligation
        // - Duty
        // - Responsibility
        // - Accountability
        // - Liability
    )
}
