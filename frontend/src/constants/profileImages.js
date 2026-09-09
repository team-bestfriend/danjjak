import baby from "../assets/icons/profile/baby.png";
import youthBoy from "../assets/icons/profile/youth_boy.png";
import youthWoman from "../assets/icons/profile/youth_woman.png";
import adultMan from "../assets/icons/profile/adult_man.png";
import adultWoman from "../assets/icons/profile/adult_woman.png";
import middleAgedMan from "../assets/icons/profile/middle_aged_man.png";
import middleAgedWoman from "../assets/icons/profile/middle_aged_woman.png";
import oldAgeMan from "../assets/icons/profile/old_age_man.png";
import oldAgeWoman from "../assets/icons/profile/old_age_woman.png";

export const PROFILE_IMAGES = {
  baby,
  youth_boy: youthBoy,
  youth_woman: youthWoman,
  adult_man: adultMan,
  adult_woman: adultWoman,
  middle_aged_man: middleAgedMan,
  middle_aged_woman: middleAgedWoman,
  old_age_man: oldAgeMan,
  old_age_woman: oldAgeWoman,
};

export function profileImageForPerson(person) {
  if (!person) return null;
  if (PROFILE_IMAGES[person.profileImageKey]) {
    return PROFILE_IMAGES[person.profileImageKey];
  }

  const relation = person.relation ?? person.relationship;
  if (relation === "아들") return adultMan;
  if (relation === "딸") return adultWoman;
  return null;
}
