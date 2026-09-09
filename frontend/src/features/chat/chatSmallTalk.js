const smallTalkRules = [
  {
    pattern: /^(?:안녕(?:하세요)?|ㅎㅇ|반가워(?:요)?|좋은\s*(?:아침|저녁)(?:이에요)?)[!?.~\s]*$/,
    reply: '안녕하세요! 무엇을 도와드릴까요?',
    showRecommendations: true,
  },
  {
    pattern: /^(?:고마워(?:요)?|감사해(?:요)?|감사합니다)[!?.~\s]*$/,
    reply: '별말씀을요! 다른 도움이 필요하면 말씀해 주세요.',
    showRecommendations: true,
  },
  {
    pattern: /^(?:잘\s*지내(?:요)?|요즘\s*어때|오늘\s*기분\s*어때)[!?.~\s]*$/,
    reply: '네, 잘 지내고 있어요. 오늘도 천천히 도와드릴게요.',
  },
  {
    pattern: /^(?:밥\s*먹었어|식사\s*했어)[!?.~\s]*$/,
    reply: '저는 밥을 먹지는 않지만, 챙겨 주셔서 고마워요!',
  },
  {
    pattern: /^(?:잘\s*가|다음에\s*봐|또\s*보자)[!?.~\s]*$/,
    reply: '네, 다음에 또 봐요! 편안한 하루 보내세요.',
  },
  {
    pattern: /^(?:너\s*누구야|누구세요|이름(?:이)?\s*뭐야|단짝이\s*뭐야)[!?.~\s]*$/,
    reply: '저는 금융 업무를 천천히 도와드리는 단짝이에요.',
  },
  {
    pattern: /^(?:뭐\s*하고\s*있어|지금\s*뭐\s*해|심심해)[!?.~\s]*$/,
    reply: '여기서 기다리고 있었어요. 필요한 게 있으면 편하게 말씀해 주세요.',
  },
  {
    pattern: /^(?:귀엽다|귀여워|멋지다|잘했어|최고야)[!?.~\s]*$/,
    reply: '고마워요! 더 편하게 도와드릴게요.',
  },
  {
    pattern: /^(?:피곤해|힘들어|속상해|외로워|기분이\s*안\s*좋아)[!?.~\s]*$/,
    reply: '그러셨군요. 잠시 쉬어 가며 천천히 하셔도 괜찮아요.',
  },
  {
    pattern: /^(?:기분\s*좋아|행복해|오늘\s*좋은\s*일이\s*있었어)[!?.~\s]*$/,
    reply: '정말 잘됐어요! 좋은 기분이 오래 갔으면 좋겠어요.',
  },
];

export function smallTalkReply(message) {
  const rule = smallTalkRules.find((candidate) => candidate.pattern.test(message.trim()));
  return rule ? { message: rule.reply, showRecommendations: Boolean(rule.showRecommendations) } : null;
}
