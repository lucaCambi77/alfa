package fragment.submissions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author luca
 *
 */
public class LucaCambi {

	private static LucaCambi instance = new LucaCambi();

	public static void main(String[] args) throws IOException {

		if (args.length == 0) {
			System.out.println(Errors.ERR_NO_FILE_INPUT);
			return;
		}

		File file = new File(args[0]);

		if (!file.exists()) {
			System.out.println(Errors.ERR_FILE_NOT_EXISTS);
			return;
		}

		try (BufferedReader in = new BufferedReader(new FileReader(file))) {
			in.lines().map(instance.new Fragments()::reassemble).forEach(System.out::println);
		}
	}

	public class Fragments {

		private final Logger log = LoggerFactory.getLogger(Fragments.class);

		private int MIN_FRAG_LENGTH = 2;
		private int MIN_FRAG_OVERLAP = -1;

		/**
		 * Algorithm to reassemble fragments input.
		 * <p>
		 * Main idea : a fragment is contained in another fragment if the end of one
		 * fragment is the beginning of the other (we check it both ways). We create
		 * chunks of fragments starting from minimum length of fragments and decreasing
		 * to 2. We do this because it is easier to find a match with bigger chunks and
		 * if we start from smaller chunks we can mismatch overlapping.
		 * 
		 * Steps :
		 * <ol>
		 * <li>Create a linkedList of fragments and find minimum fragments length</li>
		 * <li>Loop over linkedList until it has only one element</li>
		 * <li>Inside linkedList's loop we compare head of linkedList with others
		 * fragment, checking overlapping with {@link #rightToLeft} and
		 * {@link #leftToRight}
		 * <li>If a match is found, we add joined fragments and remove the match, this
		 * because add/remove operations have constant time in linkedLists. If a match
		 * is not found then it is not possible to reassemble the input
		 * <li>To avoid mismatch of fragments overlap due to the order of the
		 * linkedList, we recreate the linkedList starting from last position's mismatch
		 * until we scroll all the linkedList
		 * </ol>
		 * </p>
		 * 
		 * @param input string from file
		 * @return
		 */
		public String reassemble(String input) {

			if (input == null || (input != null && input.trim().isEmpty()))
				return Errors.ERR_INPUT_NOT_EMPTY;

			int fragmentOverlap = Integer.MAX_VALUE;

			String[] split = input.split(";");

			if (log.isDebugEnabled())
				for (String string : split) {
					log.debug(string);
				}

			Comparator<String> byLength = (e1, e2) -> e1.length() < e2.length() ? -1 : 1;

			LinkedList<String> fragments = Arrays.asList(split).stream().distinct().sorted(byLength)
					.collect(Collectors.toCollection(LinkedList::new));

			fragmentOverlap = fragments.getFirst().length();

			if (fragmentOverlap < MIN_FRAG_LENGTH)
				return Errors.ERR_MIN_FRAG_SIZE;

			MIN_FRAG_OVERLAP = fragmentOverlap;

			int fragmentsSize = fragments.size();

			int position = 0;

			while (fragments.size() > 1) {

				boolean areFragments = false;
				String popped = fragments.poll();

				if (log.isDebugEnabled())
					log.debug(popped);
				/**
				 * FIXED 07/09 : Minimum overlap size 1 instead of MIN_FRAG_LENGTH
				 */
				innerloop: while (fragmentOverlap >= 1) {

					int endPopped = popped.length() - fragmentOverlap;
					String subString = popped.substring(0, fragmentOverlap);

					for (String fragment : fragments) {

						if (log.isDebugEnabled())
							log.debug(fragment);

						if (leftToRight(fragments, popped, subString, fragment)
								|| rightToLeft(fragments, popped, endPopped, fragment)) {
							areFragments = true;
							break innerloop;
						}

					}

					fragmentOverlap--;

				}

				/**
				 * FIXED 07/09 : Avoid incorrect overlapping. Fragments can overlap in a wrong
				 * sequence leading to wrong result, so we don't give up until we scroll the
				 * whole linkedList. In the meanwhile we recreate the linkedList based on the
				 * last index
				 */

				if (!areFragments && position == fragmentsSize - 1)
					return Errors.ERR_INVALID_REASSEMBLE + input;

				if (!areFragments) {
					fragments.clear();
					List<String> safecopy = Arrays.asList(split).stream().distinct().sorted(byLength)
							.collect(Collectors.toCollection(LinkedList::new));

					fragments.addAll(safecopy.subList(position, fragmentsSize));
					fragments.addAll(safecopy.subList(0, position));
					fragmentOverlap = MIN_FRAG_OVERLAP;

				}

				position++;
			}

			return fragments.poll();
		}

		/**
		 * Reassemble two fragments right to left. We check if end of popped element is
		 * beginning of fragment.
		 * 
		 * @param fragments LinkedList
		 * @param popped    fragment
		 * @param endPopped index
		 * @param fragment  in inner loop
		 * 
		 * @return boolean if a overlap is found
		 */
		private boolean rightToLeft(LinkedList<String> fragments, String popped, int endPopped, String fragment) {
			int index = fragment.indexOf(popped.substring(endPopped));
			boolean areFragments = false;

			while (index >= 0) {

				int limit = Math.max(0, endPopped - index);

				String s1 = popped.substring(limit, endPopped);
				String s2 = fragment.substring(0, index);

				if (s1.equals(s2)) {

					areFragments = true;
					fragments.remove(fragment);
					fragments.add(popped.substring(0, endPopped - index).concat(fragment));
					break;
				}

				index = fragment.indexOf(popped, index + 1);

			}
			return areFragments;
		}

		/**
		 * Reassemble two fragments left to right. We check if end of fragment is
		 * beginning of popped element.
		 * 
		 * @param fragments LinkedList
		 * @param popped    fragment
		 * @param subString of popped
		 * @param fragment  in inner loop
		 * 
		 * @return boolean if a overlap is found
		 */
		private boolean leftToRight(LinkedList<String> fragments, String popped, String subString, String fragment) {
			int index = fragment.indexOf(subString);
			boolean areFragments = false;

			while (index >= 0) {

				int limit = Math.min(popped.length(), fragment.length() - index);

				String s1 = fragment.substring(index, index + limit);
				String s2 = popped.substring(0, limit);

				if (s1.equals(s2)) {
					areFragments = true;

					/**
					 * Edge case, popped string is contained in processed fragment, so we keep
					 * fragment
					 */
					if (fragment.contains(popped))
						break;

					fragments.remove(fragment);
					fragments.add(fragment.substring(0, index).concat(popped));
					break;
				}

				index = fragment.indexOf(subString, index + 1);
			}

			return areFragments;
		}

	}

	public final class Errors {

		public static final String ERR_NO_FILE_INPUT = "File path must be provided as input";

		public static final String ERR_FILE_NOT_EXISTS = "File does not exists";

		public static final String ERR_MIN_FRAG_SIZE = "Fragment size must be at least 2";

		public static final String ERR_INPUT_NOT_EMPTY = "Input must be not null and not empty!";

		public static final String ERR_INVALID_REASSEMBLE = "Can't reassemble fragments : ";
	}

	@Nested
	@DisplayName("Alfa Reassemble Tests")
	@TestMethodOrder(OrderAnnotation.class)
	public class ReassembleTest {

		private Fragments fragments = instance.new Fragments();

		@Test
		@Order(1)
		public void nequePorro() throws Exception {
			String s = "m quaerat voluptatem.;pora incidunt ut labore et d;, consectetur, adipisci velit;olore magnam aliqua;idunt ut labore et dolore magn;uptatem.;i dolorem ipsum qu;iquam quaerat vol;psum quia dolor sit amet, consectetur, a;ia dolor sit amet, conse;squam est, qui do;Neque porro quisquam est, qu;aerat voluptatem.;m eius modi tem;Neque porro qui;, sed quia non numquam ei;lorem ipsum quia dolor sit amet;ctetur, adipisci velit, sed quia non numq;unt ut labore et dolore magnam aliquam qu;dipisci velit, sed quia non numqua;us modi tempora incid;Neque porro quisquam est, qui dolorem i;uam eius modi tem;pora inc;am al";

			assertEquals(
					"Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet, consectetur, adipisci velit, sed "
							+ "quia non numquam eius modi tempora incidunt ut labore et dolore magnam aliquam quaerat "
							+ "voluptatem.",
					fragments.reassemble(s));
		}

		@Test
		@Order(2)
		public void oDraconian() {
			String s = "O draconia;conian devil! Oh la;h lame sa;saint!";

			assertEquals("O draconian devil! Oh lame saint!", fragments.reassemble(s));

		}

		/**
		 * Edge cases
		 * 
		 */

		@Test
		@Order(3)
		public void oneFragment() {
			String s = "Only one fragment";

			assertEquals(s, fragments.reassemble(s));

		}

		@Test
		@Order(4)
		public void inputEmptyOrNull() {

			assertEquals(Errors.ERR_INPUT_NOT_EMPTY, fragments.reassemble(null));

			assertEquals(Errors.ERR_INPUT_NOT_EMPTY, fragments.reassemble("  "));

		}

		@Test
		@Order(5)
		public void invalidInput() {
			String s = "No way to reassembl;amble this string";

			assertEquals(Errors.ERR_INVALID_REASSEMBLE + s, fragments.reassemble(s));

		}

		@Test
		@Order(6)
		public void emptyFragment() {
			String s = "This is not;;actually good";

			assertEquals(Errors.ERR_MIN_FRAG_SIZE, fragments.reassemble(s));

		}

		@Test
		@Order(7)
		public void containedString() {

			String s = "Neque porro quisquam est, qui dolorem ipsum;Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet";

			assertEquals("Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet", fragments.reassemble(s));

			String s1 = "Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet;Neque porro quisquam est, qui dolorem ipsum";

			assertEquals("Neque porro quisquam est, qui dolorem ipsum quia dolor sit amet", fragments.reassemble(s1));

		}

		/**
		 * After fix 07/09 added new test cases
		 */
		@Test
		@Order(8)
		public void fixedVersion() {

			/**
			 * Minimum overlap size 1
			 */
			assertEquals("Hello world!", fragments.reassemble("Hello;o world!"));
			assertEquals("repeated letters", fragments.reassemble("lette;ters;repeated l"));
			assertEquals("abcde", fragments.reassemble("ab;bcd;cde"));

			/**
			 * Incorrect overlapping
			 */
			assertEquals("xxefhijkabcdefh", fragments.reassemble("jkabcdefh;xxefhi;efhijk"));
			assertEquals("repeat, now let's repeat now!",
					fragments.reassemble("repeat, now;now let's repeat; repeat now!"));
			assertEquals("abcdefghabk", fragments.reassemble("abcdef;abcdef;fghab;habk"));

		}
	}
}
