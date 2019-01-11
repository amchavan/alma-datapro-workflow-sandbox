from setuptools import setup

dependencies = [
    "drawsmb"
]

setup(name='draws-mock-icd',
      version='0.1',
      description='DRAWS Message Bus API',
      url='http://www.almaobservatory.org',
      author='ALMA',
      author_email='test@alma.cl',
      license='LGPL',
      packages=['draws', 'draws/mock', 'draws/mock/messages', 'draws/mock/messages/gen'],
      install_requires=dependencies,
      zip_safe=False)
